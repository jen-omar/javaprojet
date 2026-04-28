package com.marketplace.controllers;

import com.marketplace.models.Order;
import com.marketplace.models.Product;
import com.marketplace.models.Review;
import com.marketplace.models.Wishlist;
import com.marketplace.services.OrderService;
import com.marketplace.services.ProductService;
import com.marketplace.services.ReviewService;
import com.marketplace.services.WishlistService;
import com.marketplace.services.PaymentServer;
import com.marketplace.services.StripeService;
import com.marketplace.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.List;
import com.marketplace.models.Bid;
import com.marketplace.services.BidService;
import java.util.stream.Collectors;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Group;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.AmbientLight;
import javafx.scene.PointLight;

/**
 * ClientController — vue Client:
 * • Affiche tous les produits "available" sous forme de catalogue.
 * • Permet au client d'acheter un produit (crée une Order).
 * • Barre de recherche + filtre par type.
 */
public class ClientController {

    private final ProductService productService = new ProductService();
    private final OrderService orderService = new OrderService();
    private final ReviewService reviewService = new ReviewService();
    private final WishlistService wishlistService = new WishlistService();
    private final BidService bidService = new BidService();
    private final SessionManager session = SessionManager.getInstance();

    private FlowPane cards;
    private TextField searchField;
    private ComboBox<String> filterType;
    private ComboBox<String> sortOrder; // tri par prix

    // ── Main View ─────────────────────────────────────────────────
    public Node buildView() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1a1a1a;");

        // ── Header ──────────────────────────────────────────────────
        VBox headerBox = new VBox(4);
        headerBox.setPadding(new Insets(28, 32, 0, 32));

        Label title = new Label("CATALOGUE");
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 22; -fx-font-weight: bold;");
        Label sub = new Label("Bonjour " + session.getName() + " ! Parcourez et achetez vos œuvres préférées.");
        sub.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");
        headerBox.getChildren().addAll(title, sub);

        // ── Search + Filter bar ─────────────────────────────────────
        searchField = new TextField();
        searchField.setPromptText("🔍  Rechercher par nom ou artiste…");
        searchField.setPrefWidth(260);
        searchField.setStyle(
                "-fx-control-inner-background: #2a2a2a; " +
                        "-fx-background-color: -fx-control-inner-background; " +
                        "-fx-text-fill: #ffffff; " +
                        "-fx-prompt-text-fill: derive(-fx-control-inner-background, +80%); " +
                        "-fx-border-color: #444; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8;");
        searchField.textProperty().addListener((obs, o, n) -> refreshCards());

        filterType = new ComboBox<>();
        filterType.getItems().addAll("Tous les types", "Digital Art", "Painting", "Sculpture",
                "Drawing", "Photography", "Mixed Media", "Other");
        filterType.setValue("Tous les types");
        filterType.setPrefWidth(170);
        filterType.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #444; -fx-border-radius: 6;");
        filterType.setButtonCell(darkCell());
        filterType.setOnAction(e -> refreshCards());

        // ── Tri par prix ─────────────────────────────────────────────
        sortOrder = new ComboBox<>();
        sortOrder.getItems().addAll("Prix : défaut", "Prix ↑ (croissant)", "Prix ↓ (décroissant)");
        sortOrder.setValue("Prix : défaut");
        sortOrder.setPrefWidth(175);
        sortOrder.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #444; -fx-border-radius: 6;");
        sortOrder.setButtonCell(darkCell());
        sortOrder.setOnAction(e -> refreshCards());

        HBox toolbar = new HBox(12, searchField, filterType, sortOrder);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(16, 32, 8, 32));
        HBox.setHgrow(searchField, Priority.SOMETIMES);

        // ── Cards grid ─────────────────────────────────────────────
        cards = new FlowPane();
        cards.setHgap(18);
        cards.setVgap(18);
        cards.setPadding(new Insets(16, 32, 32, 32));

        refreshCards();

        root.getChildren().addAll(headerBox, toolbar, cards);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1a1a1a; -fx-background-color: #1a1a1a; -fx-border-color: transparent;");

        // --- Chatbot UI ---
        StackPane stack = new StackPane(scroll);

        // Chat Button
        Button chatBtn = new Button("💬");
        chatBtn.setStyle(
                "-fx-background-color: #c0c0c0; -fx-text-fill: #1a1a1a; -fx-font-size: 24; -fx-background-radius: 50; -fx-cursor: hand;");
        chatBtn.setPrefSize(60, 60);
        chatBtn.setMinSize(60, 60);
        StackPane.setAlignment(chatBtn, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatBtn, new Insets(0, 30, 30, 0));

        // Chat Window
        VBox chatWindow = new VBox(0);
        chatWindow.setMaxSize(350, 450);
        chatWindow.setStyle(
                "-fx-background-color: #222; -fx-border-color: #444; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
        StackPane.setAlignment(chatWindow, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatWindow, new Insets(0, 30, 100, 0));
        chatWindow.setVisible(false);

        // Header
        HBox chatHeader = new HBox();
        chatHeader.setPadding(new Insets(10, 15, 10, 15));
        chatHeader.setStyle("-fx-background-color: #c0c0c0; -fx-background-radius: 7 7 0 0;");
        Label chatTitle = new Label("MYTHORIA ASSISTANT");
        chatTitle.setStyle("-fx-text-fill: #1a1a1a; -fx-font-weight: bold; -fx-font-size: 14;");
        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);
        Button closeChatBtn = new Button("✕");
        closeChatBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #1a1a1a; -fx-cursor: hand; -fx-font-size: 14; -fx-font-weight: bold;");
        closeChatBtn.setOnAction(e -> chatWindow.setVisible(false));
        chatHeader.getChildren().addAll(chatTitle, spacerHeader, closeChatBtn);

        // Messages area
        VBox messagesBox = new VBox(10);
        messagesBox.setPadding(new Insets(15));
        messagesBox.setStyle("-fx-background-color: #1a1a1a;");
        ScrollPane chatScroll = new ScrollPane(messagesBox);
        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background: #1a1a1a; -fx-background-color: #1a1a1a; -fx-border-color: transparent;");
        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        // Input area
        HBox inputArea = new HBox(8);
        inputArea.setPadding(new Insets(10));
        inputArea.setStyle(
                "-fx-background-color: #222; -fx-border-color: #333; -fx-border-width: 1 0 0 0; -fx-background-radius: 0 0 7 7;");
        TextField chatInput = new TextField();
        chatInput.setPromptText("Écrivez votre message...");
        chatInput.setStyle(
                "-fx-background-color: #333; -fx-text-fill: #fff; -fx-prompt-text-fill: #888; -fx-background-radius: 15; -fx-padding: 8 15;");
        HBox.setHgrow(chatInput, Priority.ALWAYS);
        Button sendBtn = new Button("➤");
        sendBtn.setStyle(
                "-fx-background-color: #c0c0c0; -fx-text-fill: #1a1a1a; -fx-background-radius: 15; -fx-cursor: hand; -fx-font-weight: bold;");

        // Add intro message
        Label intro = new Label("Bonjour " + session.getName() + " ! Comment puis-je vous aider ?");
        intro.setWrapText(true);
        intro.setStyle(
                "-fx-background-color: #333; -fx-text-fill: #fff; -fx-padding: 8 12; -fx-background-radius: 12 12 12 0;");
        HBox introRow = new HBox(intro);
        introRow.setAlignment(Pos.CENTER_LEFT);
        messagesBox.getChildren().add(introRow);

        com.marketplace.services.ChatbotService botService = new com.marketplace.services.ChatbotService();

        Runnable sendMessage = () -> {
            String text = chatInput.getText().trim();
            if (text.isEmpty())
                return;

            // User msg
            Label uMsg = new Label(text);
            uMsg.setWrapText(true);
            uMsg.setStyle(
                    "-fx-background-color: #c0c0c0; -fx-text-fill: #1a1a1a; -fx-padding: 8 12; -fx-background-radius: 12 12 0 12;");
            HBox uRow = new HBox(uMsg);
            uRow.setAlignment(Pos.CENTER_RIGHT);
            messagesBox.getChildren().add(uRow);
            chatInput.clear();

            // Typing indicator
            Label typing = new Label("L'assistant écrit...");
            typing.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
            HBox tRow = new HBox(typing);
            tRow.setAlignment(Pos.CENTER_LEFT);
            messagesBox.getChildren().add(tRow);

            // Fetch answer async
            new Thread(() -> {
                String[] result = botService.askChatbot(text, "client", session.getName());
                String reply = result[0];
                String intent = result[1];

                javafx.application.Platform.runLater(() -> {
                    messagesBox.getChildren().remove(tRow);
                    Label bMsg = new Label(reply);
                    bMsg.setWrapText(true);
                    bMsg.setStyle(
                            "-fx-background-color: #333; -fx-text-fill: #fff; -fx-padding: 8 12; -fx-background-radius: 12 12 12 0;");
                    HBox bRow = new HBox(bMsg);
                    bRow.setAlignment(Pos.CENTER_LEFT);
                    messagesBox.getChildren().add(bRow);
                    chatScroll.setVvalue(1.0); // scroll to bottom

                    // Dynamic Catalogue Actions have been removed
                    // The AI now queries the DB directly and responds with text.
                });
            }).start();
        };

        // Quick Replies Area
        FlowPane quickReplies = new FlowPane();
        quickReplies.setHgap(8);
        quickReplies.setVgap(8);
        quickReplies.setPadding(new Insets(10, 15, 0, 15));
        quickReplies.setStyle("-fx-background-color: #222;");

        String[] quickOptions = { "🎨 Montrer peintures", "🛒 Mes commandes", "🌟 Nouveautés", "❓ Aide" };
        for (String opt : quickOptions) {
            Button chip = new Button(opt);
            chip.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #c0c0c0; -fx-border-color: #555; -fx-border-radius: 12; -fx-cursor: hand; -fx-font-size: 11;");
            chip.setOnMouseEntered(e -> chip.setStyle(
                    "-fx-background-color: #333; -fx-text-fill: #fff; -fx-border-color: #777; -fx-border-radius: 12; -fx-cursor: hand; -fx-font-size: 11;"));
            chip.setOnMouseExited(e -> chip.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #c0c0c0; -fx-border-color: #555; -fx-border-radius: 12; -fx-cursor: hand; -fx-font-size: 11;"));
            chip.setOnAction(e -> {
                chatInput.setText(opt);
                sendMessage.run();
            });
            quickReplies.getChildren().add(chip);
        }

        sendBtn.setOnAction(e -> sendMessage.run());
        chatInput.setOnAction(e -> sendMessage.run());

        inputArea.getChildren().addAll(chatInput, sendBtn);
        chatWindow.getChildren().addAll(chatHeader, chatScroll, quickReplies, inputArea);

        chatBtn.setOnAction(e -> chatWindow.setVisible(!chatWindow.isVisible()));

        stack.getChildren().addAll(chatBtn, chatWindow);

        return stack;
    }

    // ── Refresh with search + filter + sort ─────────────────────
    private void refreshCards() {
        cards.getChildren().clear();
        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        String type = filterType != null ? filterType.getValue() : "Tous les types";
        String sort = sortOrder != null ? sortOrder.getValue() : "Prix : défaut";

        List<Product> products = productService.getAll().stream()
                .filter(p -> "available".equals(p.getStatus()))
                .filter(p -> query.isBlank()
                        || p.getName().toLowerCase().contains(query)
                        || (p.getArtistName() != null && p.getArtistName().toLowerCase().contains(query)))
                .filter(p -> "Tous les types".equals(type) || type.equals(p.getType()))
                .sorted((a, b) -> {
                    if ("Prix ↑ (croissant)".equals(sort)) {
                        BigDecimal pa = a.getPrice() != null ? a.getPrice() : BigDecimal.ZERO;
                        BigDecimal pb = b.getPrice() != null ? b.getPrice() : BigDecimal.ZERO;
                        return pa.compareTo(pb);
                    } else if ("Prix ↓ (décroissant)".equals(sort)) {
                        BigDecimal pa = a.getPrice() != null ? a.getPrice() : BigDecimal.ZERO;
                        BigDecimal pb = b.getPrice() != null ? b.getPrice() : BigDecimal.ZERO;
                        return pb.compareTo(pa);
                    }
                    return 0; // défaut: ordre DB
                })
                .collect(Collectors.toList());

        if (products.isEmpty()) {
            Label empty = new Label("Aucun produit disponible pour votre recherche.");
            empty.setStyle("-fx-text-fill: #555; -fx-font-size: 13;");
            cards.getChildren().add(empty);
            return;
        }
        for (Product p : products)
            cards.getChildren().add(buildCard(p));
    }

    // ── Product card (client view) ────────────────────────────────
    private Node buildCard(Product p) {
        VBox card = new VBox(0);
        card.setPrefWidth(270);
        card.setStyle(
                "-fx-background-color: #222; -fx-background-radius: 12; " +
                        "-fx-border-color: #2e2e2e; -fx-border-radius: 12; -fx-border-width: 1;");

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #252525; -fx-background-radius: 12; " +
                        "-fx-border-color: #c0c0c0; -fx-border-radius: 12; -fx-border-width: 1;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: #222; -fx-background-radius: 12; " +
                        "-fx-border-color: #2e2e2e; -fx-border-radius: 12; -fx-border-width: 1;"));

        // Image — clickable to open 3D museum view
        String rawUrl = p.getImageUrl();
        if (rawUrl != null && !rawUrl.isBlank()) {
            try {
                String url = rawUrl.startsWith("http") || rawUrl.startsWith("file:") ? rawUrl
                        : "http://localhost" + rawUrl;
                Image img = new Image(url, 270, 150, false, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(270);
                iv.setFitHeight(150);
                iv.setPreserveRatio(false);
                iv.setStyle("-fx-background-radius: 12 12 0 0;");
                iv.setCursor(Cursor.HAND);
                iv.setOnMouseClicked(e -> show3DMuseumView(p));
                img.errorProperty().addListener((obs, old, err) -> {
                    if (err)
                        card.getChildren().remove(iv);
                });
                card.getChildren().add(iv);
            } catch (Exception ignored) {
            }
        } else {
            // No-image placeholder — also clickable
            StackPane placeholder = new StackPane();
            placeholder.setPrefSize(270, 150);
            placeholder.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 12 12 0 0;");
            Label noImg = new Label("🖼  Pas de photo");
            noImg.setStyle("-fx-text-fill: #555; -fx-font-size: 13;");
            placeholder.getChildren().add(noImg);
            placeholder.setCursor(Cursor.HAND);
            placeholder.setOnMouseClicked(e -> show3DMuseumView(p));
            card.getChildren().add(placeholder);
        }

        // Content
        VBox content = new VBox(8);
        content.setPadding(new Insets(14, 16, 16, 16));

        Label name = new Label(p.getName());
        name.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 14; -fx-cursor: hand;");
        name.setWrapText(true);
        name.setOnMouseClicked(e -> show3DMuseumView(p));

        Label artist = new Label("par " + (p.getArtistName() != null ? p.getArtistName() : "—"));
        artist.setStyle("-fx-text-fill: #777; -fx-font-size: 11;");

        Label price = new Label(p.getPrice() != null ? p.getPrice().toPlainString() + " €" : "—");
        price.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 18; -fx-font-weight: bold;");

        String d = p.getDescription() != null && !p.getDescription().isBlank()
                ? p.getDescription()
                : "Aucune description.";
        Label desc = new Label(d.length() > 70 ? d.substring(0, 70) + "…" : d);
        desc.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");
        desc.setWrapText(true);

        HBox tags = new HBox(6);
        tags.getChildren().addAll(miniTag(p.getType()), miniTag(p.getSaleType()));

        // Action button
        Button actionBtn;
        if ("auction".equalsIgnoreCase(p.getSaleType())) {
            actionBtn = new Button("⚡  Enchérir");
            actionBtn.setOnAction(e -> showBidDialog(p));
        } else {
            actionBtn = new Button("🛒  Acheter");
            actionBtn.setOnAction(e -> showBuyDialog(p));
        }
        actionBtn.setPrefWidth(Double.MAX_VALUE);
        actionBtn.setStyle(
                "-fx-background-color: #c0c0c0; -fx-text-fill: #111; -fx-font-weight: bold; " +
                        "-fx-background-radius: 7; -fx-padding: 9 0; -fx-cursor: hand; -fx-font-size: 13;");
        actionBtn.setOnMouseEntered(e -> actionBtn.setStyle(
                "-fx-background-color: #dcdcdc; -fx-text-fill: #111; -fx-font-weight: bold; " +
                        "-fx-background-radius: 7; -fx-padding: 9 0; -fx-cursor: hand; -fx-font-size: 13;"));
        actionBtn.setOnMouseExited(e -> actionBtn.setStyle(
                "-fx-background-color: #c0c0c0; -fx-text-fill: #111; -fx-font-weight: bold; " +
                        "-fx-background-radius: 7; -fx-padding: 9 0; -fx-cursor: hand; -fx-font-size: 13;"));

        // ── ♥ Wishlist button ────────────────────────────────────
        Button wishBtn = new Button("♥  Souhait");
        boolean alreadyWished = wishlistService.isAlreadyInWishlist(session.getName(), p.getId());
        if (alreadyWished) {
            wishBtn.setText("♥  Ajouté");
            wishBtn.setStyle("-fx-background-color: #3a2a1a; -fx-text-fill: #c0c0c0; " +
                    "-fx-background-radius: 7; -fx-padding: 7 12; -fx-cursor: default; -fx-font-size: 12; " +
                    "-fx-border-color: #c0c0c0; -fx-border-radius: 7; -fx-border-width: 1;");
            wishBtn.setDisable(true);
        } else {
            wishBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #c0c0c0; " +
                    "-fx-border-color: #c0c0c0; -fx-border-radius: 7; -fx-background-radius: 7; " +
                    "-fx-padding: 7 12; -fx-cursor: hand; -fx-font-size: 12;");
            wishBtn.setOnMouseEntered(e -> wishBtn.setStyle(
                    "-fx-background-color: #2a1e0e; -fx-text-fill: #dcdcdc; " +
                            "-fx-border-color: #dcdcdc; -fx-border-radius: 7; -fx-background-radius: 7; " +
                            "-fx-padding: 7 12; -fx-cursor: hand; -fx-font-size: 12;"));
            wishBtn.setOnMouseExited(e -> wishBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #c0c0c0; " +
                            "-fx-border-color: #c0c0c0; -fx-border-radius: 7; -fx-background-radius: 7; " +
                            "-fx-padding: 7 12; -fx-cursor: hand; -fx-font-size: 12;"));
            wishBtn.setOnAction(e -> {
                Wishlist w = new Wishlist();
                w.setClientName(session.getName());
                w.setProductId(p.getId());
                wishlistService.add(w);
                wishBtn.setText("♥  Ajouté ✓");
                wishBtn.setStyle("-fx-background-color: #1a3a1a; -fx-text-fill: #7ec97e; " +
                        "-fx-border-color: #3a8a3a; -fx-border-radius: 7; -fx-background-radius: 7; " +
                        "-fx-padding: 7 12; -fx-cursor: default; -fx-font-size: 12;");
                wishBtn.setDisable(true);
            });
        }

        // ── ★ Avis button ────────────────────────────────────────
        Button avisBtn = new Button("★  Avis");
        avisBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #aaa; " +
                "-fx-border-color: #444; -fx-border-radius: 7; -fx-background-radius: 7; " +
                "-fx-padding: 7 12; -fx-cursor: hand; -fx-font-size: 12;");
        avisBtn.setOnMouseEntered(e -> avisBtn.setStyle(
                "-fx-background-color: #2a2a2a; -fx-text-fill: #fff; " +
                        "-fx-border-color: #666; -fx-border-radius: 7; -fx-background-radius: 7; " +
                        "-fx-padding: 7 12; -fx-cursor: hand; -fx-font-size: 12;"));
        avisBtn.setOnMouseExited(e -> avisBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #aaa; " +
                        "-fx-border-color: #444; -fx-border-radius: 7; -fx-background-radius: 7; " +
                        "-fx-padding: 7 12; -fx-cursor: hand; -fx-font-size: 12;"));
        avisBtn.setOnAction(e -> showAvisDialog(p));

        // ── Row 2: wish + avis — HGrow splits space, NO MAX_VALUE ─
        HBox secondRow = new HBox(8, wishBtn, avisBtn);
        secondRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(wishBtn, Priority.ALWAYS);
        HBox.setHgrow(avisBtn, Priority.ALWAYS);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        content.getChildren().addAll(name, artist, price, desc, tags, spacer, actionBtn, secondRow);
        card.getChildren().add(content);
        return card;
    }

    // ── Bid dialog ───────────────────────────────────────────────
    private void showBidDialog(Product p) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Enchérir sur : " + p.getName());
        dialog.setWidth(450);
        dialog.setResizable(false);

        VBox layout = new VBox(14);
        layout.setPadding(new Insets(24, 32, 28, 32));
        layout.setStyle("-fx-background-color: #1a1a1a;");

        Label title = new Label("Historique des Enchères");
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 18; -fx-font-weight: bold;");

        // History view
        VBox historyBox = new VBox(8);
        historyBox.setPadding(new Insets(10));
        historyBox.setStyle(
                "-fx-background-color: #222; -fx-border-color: #333; -fx-border-radius: 8; -fx-background-radius: 8;");
        historyBox.setPrefHeight(150);

        List<Bid> bids = bidService.getByProductId(p.getId());
        BigDecimal maxBid = p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;

        if (bids.isEmpty()) {
            Label noBids = new Label("Aucune enchère pour le moment. Prix de base : " + maxBid.toPlainString() + " €");
            noBids.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
            historyBox.getChildren().add(noBids);
        } else {
            for (int i = 0; i < bids.size(); i++) {
                Bid b = bids.get(i);
                if (i == 0 && b.getAmount().compareTo(maxBid) > 0) {
                    maxBid = b.getAmount();
                }

                HBox bidRow = new HBox(10);
                bidRow.setAlignment(Pos.CENTER_LEFT);

                Label bName = new Label(b.getBidderName());
                bName.setStyle("-fx-text-fill: #ddd; -fx-font-weight: bold; -fx-font-size: 13;");
                bName.setPrefWidth(120);

                Label bAmt = new Label(b.getAmount().toPlainString() + " €");
                bAmt.setStyle("-fx-text-fill: #c0c0c0; -fx-font-weight: bold; -fx-font-size: 14;");
                bAmt.setPrefWidth(80);

                String dateStr = b.getCreatedAt() != null
                        ? b.getCreatedAt().toString().replace("T", " ").substring(0, 16)
                        : "";
                Label bDate = new Label(dateStr);
                bDate.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");

                bidRow.getChildren().addAll(bName, bAmt, bDate);
                historyBox.getChildren().add(bidRow);

                if (i < bids.size() - 1) {
                    Separator sep = new Separator();
                    sep.setStyle("-fx-background-color: #333;");
                    historyBox.getChildren().add(sep);
                }
            }
        }

        ScrollPane scroll = new ScrollPane(historyBox);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(160);
        scroll.setStyle("-fx-background: #222; -fx-background-color: #222; -fx-border-color: transparent;");

        // Bid input
        Label lblAmount = new Label("Votre offre (€) *");
        lblAmount.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
        TextField tfAmount = clientField("");
        tfAmount.setPromptText("Ex: " + maxBid.add(new BigDecimal("5.0")).toPlainString());

        Label errLbl = new Label();
        errLbl.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11;");

        Button submitBtn = dialogGoldBtn("⚡ Placer l'enchère");
        Button cancelBtn = dialogOutlineBtn("✕ Annuler");
        cancelBtn.setOnAction(e -> dialog.close());

        final BigDecimal currentMax = maxBid;
        submitBtn.setOnAction(e -> {
            try {
                BigDecimal amt = new BigDecimal(tfAmount.getText().trim());
                if (amt.compareTo(currentMax) <= 0) {
                    setErr(tfAmount);
                    errLbl.setText("L'offre doit être supérieure à " + currentMax.toPlainString() + " €");
                    return;
                }

                Bid newBid = new Bid(session.getName(), amt, p.getId());
                bidService.add(newBid);

                dialog.close();
                // Show a success message
                Stage success = new Stage();
                success.initModality(Modality.APPLICATION_MODAL);
                success.setTitle("Enchère placée");
                success.setWidth(350);
                VBox box = new VBox(16);
                box.setAlignment(Pos.CENTER);
                box.setPadding(new Insets(30));
                box.setStyle("-fx-background-color: #1a1a1a;");
                Label sTitle = new Label("Offre enregistrée !");
                sTitle.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 18; -fx-font-weight: bold;");
                Label sMsg = new Label("Votre enchère de " + amt.toPlainString() + " € a été placée.");
                sMsg.setStyle("-fx-text-fill: #aaa; -fx-font-size: 13;");
                Button ok = dialogGoldBtn("Fermer");
                ok.setOnAction(ev -> success.close());
                box.getChildren().addAll(sTitle, sMsg, ok);
                success.setScene(new Scene(box));
                success.show();

            } catch (NumberFormatException ex) {
                setErr(tfAmount);
                errLbl.setText("Veuillez entrer un montant valide.");
            }
        });

        HBox btnRow = new HBox(12, submitBtn, cancelBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(submitBtn, Priority.ALWAYS);
        HBox.setHgrow(cancelBtn, Priority.ALWAYS);

        layout.getChildren().addAll(title, scroll, lblAmount, tfAmount, errLbl, btnRow);
        dialog.setScene(new Scene(layout));
        dialog.show();
    }

    // ── Purchase dialog ──────────────────────────────────────────
    private void showBuyDialog(Product p) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Confirmer l'achat");
        dialog.setWidth(420);
        dialog.setResizable(false);

        VBox layout = new VBox(18);
        layout.setPadding(new Insets(30, 32, 28, 32));
        layout.setStyle("-fx-background-color: #1a1a1a;");

        // Title
        Label title = new Label("Confirmer l'achat");
        title.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 18; -fx-font-weight: bold;");

        // Product summary box
        VBox summaryBox = new VBox(8);
        summaryBox.setPadding(new Insets(16));
        summaryBox.setStyle(
                "-fx-background-color: #2a2a2a; -fx-background-radius: 10; " +
                        "-fx-border-color: #333; -fx-border-radius: 10; -fx-border-width: 1;");

        Label pName = new Label(p.getName());
        pName.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14; -fx-font-weight: bold;");
        pName.setWrapText(true);
        Label pArtist = new Label("Artiste : " + (p.getArtistName() != null ? p.getArtistName() : "—"));
        pArtist.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
        Label pPrice = new Label("Prix : " + (p.getPrice() != null ? p.getPrice().toPlainString() + " €" : "—"));
        pPrice.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 15; -fx-font-weight: bold;");
        Label pType = new Label("Type : " + p.getType() + "   ·   " + p.getSaleType());
        pType.setStyle("-fx-text-fill: #555; -fx-font-size: 11;");
        summaryBox.getChildren().addAll(pName, pArtist, pPrice, pType);

        // Buyer name (pre-filled from session)
        Label buyerLabel = new Label("Votre nom *");
        buyerLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
        TextField tfBuyer = clientField(session.getName());

        // Error
        Label errLbl = new Label("");
        errLbl.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11;");

        // Buttons — fixed width so text is always visible
        Button confirmBtn = dialogGoldBtn("✓  Confirmer l'achat");
        Button cancelBtn = dialogOutlineBtn("✕  Annuler");
        cancelBtn.setOnAction(e -> dialog.close());

        confirmBtn.setOnAction(e -> {
            String buyer = tfBuyer.getText().trim();
            if (buyer.isBlank()) {
                setErr(tfBuyer);
                errLbl.setText("Votre nom est requis.");
                return;
            }
            if (buyer.length() < 2) {
                setErr(tfBuyer);
                errLbl.setText("Nom trop court (min 2 caractères).");
                return;
            }

            confirmBtn.setDisable(true);
            cancelBtn.setDisable(true);
            errLbl.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 12;");
            errLbl.setText("⏳  Ouverture de Stripe... (Veuillez compléter le paiement dans votre navigateur)");

            try {
                PaymentServer paymentServer = new PaymentServer(
                        () -> javafx.application.Platform.runLater(() -> {
                            // Success callback
                            Order order = new Order();
                            order.setBuyerName(buyer);
                            order.setPrice(p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO);
                            order.setOrderType(p.getSaleType() != null ? p.getSaleType() : "fixed");
                            order.setProductId(p.getId());
                            orderService.add(order);

                            productService.markAsSold(p.getId());

                            dialog.close();
                            showSuccessDialog(p, buyer);
                            refreshCards();
                        }),
                        () -> javafx.application.Platform.runLater(() -> {
                            // Cancel callback
                            confirmBtn.setDisable(false);
                            cancelBtn.setDisable(false);
                            errLbl.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11;");
                            errLbl.setText("Paiement annulé. Vous pouvez réessayer.");
                        }));

                paymentServer.start();
                int port = paymentServer.getPort();
                StripeService stripeService = new StripeService();
                String url = stripeService.createCheckoutSession(p, "http://localhost:" + port + "/success",
                        "http://localhost:" + port + "/cancel");

                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception ex) {
                confirmBtn.setDisable(false);
                cancelBtn.setDisable(false);
                errLbl.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11;");
                errLbl.setText("Erreur Stripe: " + ex.getMessage());
            }
        });

        HBox btnRow = new HBox(12, confirmBtn, cancelBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(confirmBtn, Priority.ALWAYS);
        HBox.setHgrow(cancelBtn, Priority.ALWAYS);

        layout.getChildren().addAll(title, summaryBox, buyerLabel, tfBuyer, errLbl, btnRow);
        dialog.setScene(new Scene(layout));
        dialog.show();
    }

    // ── Success dialog ───────────────────────────────────────────
    private void showSuccessDialog(Product p, String buyer) {
        Stage success = new Stage();
        success.initModality(Modality.APPLICATION_MODAL);
        success.setTitle("Achat confirmé !");
        success.setWidth(380);
        success.setResizable(false);

        VBox layout = new VBox(16);
        layout.setPadding(new Insets(32, 36, 28, 36));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #1a1a1a;");

        Label icon = new Label("✅");
        icon.setStyle("-fx-font-size: 42;");

        Label msg1 = new Label("Commande confirmée !");
        msg1.setStyle("-fx-text-fill: #a8d5a2; -fx-font-size: 18; -fx-font-weight: bold;");

        Label msg2 = new Label(
                "Merci " + buyer + " !\nVotre achat de « " + p.getName() + " »\na été enregistré avec succès.");
        msg2.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
        msg2.setWrapText(true);
        msg2.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button closeBtn = goldBtn("Fermer");
        closeBtn.setOnAction(e -> success.close());

        layout.getChildren().addAll(icon, msg1, msg2, closeBtn);
        success.setScene(new Scene(layout));
        success.show();
    }

    // ── Avis dialog ──────────────────────────────────────────────
    private void showAvisDialog(Product p) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Laisser un avis");
        dialog.setWidth(440);
        dialog.setResizable(false);

        VBox layout = new VBox(16);
        layout.setPadding(new Insets(28, 32, 28, 32));
        layout.setStyle("-fx-background-color: #1a1a1a;");

        // Title
        Label title = new Label("★  Laisser un avis");
        title.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 18; -fx-font-weight: bold;");

        // Product name
        Label prodLbl = new Label("« " + p.getName() + " »");
        prodLbl.setStyle("-fx-text-fill: #777; -fx-font-size: 12;");
        prodLbl.setWrapText(true);

        // ── Star rating ───────────────────────────────────────────
        Label ratingLbl = new Label("Note *");
        ratingLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");

        final int[] selectedRating = { 0 };
        Button[] stars = new Button[5];
        HBox starRow = new HBox(6);
        starRow.setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < 5; i++) {
            final int val = i + 1;
            Button star = new Button("☆");
            star.setStyle("-fx-background-color: transparent; -fx-text-fill: #555; " +
                    "-fx-font-size: 24; -fx-padding: 0 4; -fx-cursor: hand;");
            star.setOnAction(e -> {
                selectedRating[0] = val;
                // Update all stars display
                for (int j = 0; j < 5; j++) {
                    boolean filled = (j < val);
                    stars[j].setText(filled ? "★" : "☆");
                    stars[j].setStyle("-fx-background-color: transparent; " +
                            "-fx-text-fill: " + (filled ? "#c9a84c" : "#555") + "; " +
                            "-fx-font-size: 24; -fx-padding: 0 4; -fx-cursor: hand;");
                }
            });
            stars[i] = star;
            starRow.getChildren().add(star);
        }

        // ── Comment ─────────────────────────────────────────────
        Label commentLbl = new Label("Commentaire (optionnel)");
        commentLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");

        TextArea taComment = new TextArea();
        taComment.setPromptText("Partagez votre expérience…");
        taComment.setPrefRowCount(3);
        taComment.setWrapText(true);
        taComment.setStyle(
                "-fx-control-inner-background: #2a2a2a; " +
                        "-fx-background-color: -fx-control-inner-background; " +
                        "-fx-text-fill: #ffffff; " +
                        "-fx-prompt-text-fill: derive(-fx-control-inner-background, +80%); " +
                        "-fx-border-color: #444; -fx-border-radius: 6; -fx-background-radius: 6;");

        // ── Error label ──────────────────────────────────────────
        Label errLbl = new Label("");
        errLbl.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11;");

        // ── Buttons ──────────────────────────────────────────────
        Button submitBtn = dialogGoldBtn("★  Envoyer l'avis");
        Button cancelBtn = dialogOutlineBtn("✕  Annuler");
        cancelBtn.setOnAction(e -> dialog.close());

        submitBtn.setOnAction(e -> {
            if (selectedRating[0] == 0) {
                errLbl.setText("Veuillez choisir une note (1 à 5 étoiles).");
                return;
            }
            String comment = taComment.getText().trim();

            Review review = new Review();
            review.setReviewerName(session.getName());
            review.setRating(selectedRating[0]);
            review.setComment(comment.isBlank() ? null : comment);
            review.setProductId(p.getId());
            reviewService.add(review);

            dialog.close();
            // Feedback
            Stage ok = new Stage();
            ok.initModality(Modality.APPLICATION_MODAL);
            ok.setTitle("Avis envoyé !");
            ok.setWidth(320);
            ok.setResizable(false);
            VBox okBox = new VBox(14);
            okBox.setPadding(new Insets(28, 28, 24, 28));
            okBox.setAlignment(Pos.CENTER);
            okBox.setStyle("-fx-background-color: #1a1a1a;");
            Label okIcon = new Label("✅");
            okIcon.setStyle("-fx-font-size: 36;");
            String stars2 = "★".repeat(selectedRating[0]) + "☆".repeat(5 - selectedRating[0]);
            Label okLbl = new Label("Merci pour votre avis !\n" + stars2);
            okLbl.setStyle("-fx-text-fill: #a8d5a2; -fx-font-size: 14; -fx-font-weight: bold;");
            okLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            Button okClose = dialogGoldBtn("Fermer");
            okClose.setOnAction(ev -> ok.close());
            okBox.getChildren().addAll(okIcon, okLbl, okClose);
            ok.setScene(new Scene(okBox));
            ok.show();
        });

        HBox btnRow = new HBox(12, submitBtn, cancelBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(submitBtn, Priority.ALWAYS);
        HBox.setHgrow(cancelBtn, Priority.ALWAYS);

        layout.getChildren().addAll(title, prodLbl, ratingLbl, starRow, commentLbl, taComment, errLbl, btnRow);
        dialog.setScene(new Scene(layout));
        dialog.show();
    }

    // ── 3D Showroom (Native JavaFX 3D) ───────────────────────────
    private void show3DMuseumView(Product product) {
        // Create 3D Stage
        Stage stage = new Stage();
        stage.setTitle("Galerie 3D — " + product.getName());
        stage.initModality(Modality.APPLICATION_MODAL);

        // --- 3D Scene Setup ---
        Group root3D = new Group();
        Group room = new Group(); // Group for the environment
        
        // Materials
        PhongMaterial wallMat = new PhongMaterial(Color.web("#2a2c30")); // Lighter grey to see geometry
        PhongMaterial floorMat = new PhongMaterial(Color.web("#111111")); // Dark marble-like floor
        PhongMaterial goldFrameMat = new PhongMaterial(Color.web("#d4af37")); // Gold Frame
        goldFrameMat.setSpecularColor(Color.WHITE);
        
        // Floor
        Box floor = new Box(1500, 2, 1500);
        floor.setMaterial(floorMat);
        floor.setTranslateY(300);

        // Ceiling
        Box ceiling = new Box(1500, 2, 1500);
        ceiling.setMaterial(new PhongMaterial(Color.web("#0a0a0a")));
        ceiling.setTranslateY(-400);

        // Walls (Left, Right, Back)
        Box backWall = new Box(1500, 800, 2);
        backWall.setMaterial(wallMat);
        backWall.setTranslateZ(600);
        backWall.setTranslateY(-50);

        Box leftWall = new Box(2, 800, 1500);
        leftWall.setMaterial(new PhongMaterial(Color.web("#202225")));
        leftWall.setTranslateX(-600);
        leftWall.setTranslateY(-50);

        Box rightWall = new Box(2, 800, 1500);
        rightWall.setMaterial(new PhongMaterial(Color.web("#202225")));
        rightWall.setTranslateX(600);
        rightWall.setTranslateY(-50);

        // Baseboards (to see the room corners)
        Box bbBack = new Box(1500, 20, 10);
        bbBack.setMaterial(new PhongMaterial(Color.web("#0a0a0a")));
        bbBack.setTranslateY(290); bbBack.setTranslateZ(595);
        
        Box bbLeft = new Box(10, 20, 1500);
        bbLeft.setMaterial(new PhongMaterial(Color.web("#0a0a0a")));
        bbLeft.setTranslateY(290); bbLeft.setTranslateX(-595);

        Box bbRight = new Box(10, 20, 1500);
        bbRight.setMaterial(new PhongMaterial(Color.web("#0a0a0a")));
        bbRight.setTranslateY(290); bbRight.setTranslateX(595);
        
        // The Painting Frame
        Box frame = new Box(340, 260, 20);
        frame.setMaterial(goldFrameMat);
        frame.setTranslateZ(580);
        frame.setTranslateY(-60);

        // The Painting Canvas
        Box canvas = new Box(300, 220, 5);
        PhongMaterial canvasMat = new PhongMaterial();
        String rawUrl = product.getImageUrl();
        if (rawUrl != null && !rawUrl.isBlank()) {
            try {
                String urlStr = rawUrl.startsWith("http") || rawUrl.startsWith("file:") ? rawUrl : "http://localhost" + rawUrl;
                if (!urlStr.startsWith("http") && !urlStr.startsWith("file:")) {
                    java.io.File f = new java.io.File(rawUrl);
                    if (f.exists()) urlStr = f.toURI().toString();
                }
                Image img = new Image(urlStr, true);
                canvasMat.setDiffuseMap(img);
            } catch (Exception e) {
                canvasMat.setDiffuseColor(Color.DARKGRAY);
            }
        } else {
            canvasMat.setDiffuseColor(Color.DARKGRAY);
        }
        canvas.setMaterial(canvasMat);
        canvas.setTranslateZ(568);
        canvas.setTranslateY(-60);

        // Bench
        Group bench = new Group();
        Box seat = new Box(240, 25, 80);
        seat.setMaterial(new PhongMaterial(Color.web("#111")));
        Box leg1 = new Box(15, 60, 60); leg1.setTranslateX(-100); leg1.setTranslateY(40);
        Box leg2 = new Box(15, 60, 60); leg2.setTranslateX(100); leg2.setTranslateY(40);
        bench.getChildren().addAll(seat, leg1, leg2);
        bench.setTranslateY(200); bench.setTranslateZ(0);

        // --- Lights ---
        AmbientLight ambient = new AmbientLight(Color.rgb(100, 100, 120, 0.6));
        PointLight pointLight = new PointLight(Color.web("#fff5e6"));
        pointLight.setTranslateZ(250);
        pointLight.setTranslateY(-200);
        pointLight.setTranslateX(0);

        room.getChildren().addAll(floor, ceiling, backWall, leftWall, rightWall, bbBack, bbLeft, bbRight, frame, canvas, bench);
        root3D.getChildren().addAll(room, ambient, pointLight);

        // --- Camera ---
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(5000.0);
        camera.setTranslateZ(-1000);
        camera.setTranslateY(-100);

        SubScene subScene = new SubScene(root3D, 1100, 750, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);
        subScene.setFill(Color.BLACK);

        // Default rotation to show 3D immediately
        Rotate rotateX = new Rotate(-10, Rotate.X_AXIS);
        Rotate rotateY = new Rotate(20, Rotate.Y_AXIS);
        room.getTransforms().addAll(rotateX, rotateY);

        final double[] lastMouseX = {0};
        final double[] lastMouseY = {0};

        subScene.setOnMousePressed(e -> {
            lastMouseX[0] = e.getSceneX();
            lastMouseY[0] = e.getSceneY();
        });

        subScene.setOnMouseDragged(e -> {
            double deltaX = e.getSceneX() - lastMouseX[0];
            double deltaY = e.getSceneY() - lastMouseY[0];
            rotateY.setAngle(rotateY.getAngle() + deltaX * 0.3);
            rotateX.setAngle(rotateX.getAngle() - deltaY * 0.3);
            lastMouseX[0] = e.getSceneX();
            lastMouseY[0] = e.getSceneY();
        });

        // UI Overlay
        VBox info = new VBox(10);
        info.setPadding(new Insets(30));
        info.setStyle("-fx-background-color: rgba(20,20,20,0.85); -fx-background-radius: 0 20 0 0;");
        info.setOpacity(0); // Hidden by default
        
        Label lTitle = new Label(product.getName());
        lTitle.setStyle("-fx-text-fill: white; -fx-font-size: 26; -fx-font-weight: bold;");
        Label lArt = new Label("Oeuvre de " + (product.getArtistName()!=null?product.getArtistName():"Anonyme"));
        lArt.setStyle("-fx-text-fill: #d4af37; -fx-font-size: 16; -fx-font-style: italic;");
        Label lPrice = new Label(product.getPrice() + " €");
        lPrice.setStyle("-fx-text-fill: #fff; -fx-font-size: 20;");
        
        Button backBtn = new Button("← Sortir de la galerie");
        backBtn.setStyle("-fx-background-color: #d4af37; -fx-text-fill: black; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 20; -fx-background-radius: 5;");
        backBtn.setOnAction(e -> stage.close());
        
        info.getChildren().addAll(lTitle, lArt, lPrice, new Separator(), backBtn);

        // Movement with Scroll (Forward/Backward)
        subScene.setOnScroll(e -> {
            double delta = e.getDeltaY();
            double newZ = camera.getTranslateZ() + delta * 2;
            if (newZ > 450) newZ = 450; 
            if (newZ < -1500) newZ = -1500;
            camera.setTranslateZ(newZ);
            info.setOpacity(newZ > -150 ? 1 : 0);
        });
        
        StackPane root = new StackPane(subScene, info);
        StackPane.setAlignment(info, Pos.BOTTOM_LEFT);
        
        Scene scene = new Scene(root, 1100, 750);
        
        // Full Key Movement (WASD / Arrows)
        scene.setOnKeyPressed(e -> {
            double moveAmount = 15.0;
            switch (e.getCode()) {
                case W, UP -> camera.setTranslateZ(Math.min(450, camera.getTranslateZ() + moveAmount));
                case S, DOWN -> camera.setTranslateZ(Math.max(-1500, camera.getTranslateZ() - moveAmount));
                case A, LEFT -> camera.setTranslateX(Math.max(-450, camera.getTranslateX() - moveAmount));
                case D, RIGHT -> camera.setTranslateX(Math.min(450, camera.getTranslateX() + moveAmount));
            }
            // Update info visibility
            info.setOpacity(camera.getTranslateZ() > -150 ? 1 : 0);
        });

        stage.setScene(scene);
        stage.show();
    }

    /** Escape a string for safe embedding in JavaScript */
    private String escJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "").replace("\t", " ");
    }

    // ── UI Helpers ───────────────────────────────────────────────

    private TextField clientField(String value) {
        TextField tf = new TextField(value);
        tf.setStyle(
                "-fx-control-inner-background: #2a2a2a; " +
                        "-fx-background-color: -fx-control-inner-background; " +
                        "-fx-text-fill: #ffffff; " +
                        "-fx-prompt-text-fill: derive(-fx-control-inner-background, +80%); " +
                        "-fx-border-color: #444; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 9;");
        tf.focusedProperty().addListener((obs, wasF, isF) -> {
            if (!tf.getStyle().contains("#ff6b6b")) {
                tf.setStyle(isF
                        ? tf.getStyle().replace("-fx-border-color: #444;",
                                "-fx-border-color: #c9a84c; -fx-border-width: 1.5;")
                        : tf.getStyle()
                                .replace("-fx-border-color: #c9a84c; -fx-border-width: 1.5;",
                                        "-fx-border-color: #444;"));
            }
        });
        return tf;
    }

    private void setErr(TextField tf) {
        tf.setStyle(
                "-fx-control-inner-background: #2a2a2a; " +
                        "-fx-background-color: -fx-control-inner-background; " +
                        "-fx-text-fill: #ffffff; " +
                        "-fx-border-color: #ff6b6b; -fx-border-width: 1.5; " +
                        "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 9;");
    }

    // ── Button helpers for CARDS (full width) ────────────────────
    private Button goldBtn(String text) {
        Button b = new Button(text);
        b.setPrefWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color: #c9a84c; -fx-text-fill: #111; -fx-font-weight: bold; " +
                "-fx-background-radius: 7; -fx-padding: 9 0; -fx-cursor: hand; -fx-font-size: 13;");
        b.setOnMouseEntered(
                e -> b.setStyle("-fx-background-color: #e0be6a; -fx-text-fill: #111; -fx-font-weight: bold; " +
                        "-fx-background-radius: 7; -fx-padding: 9 0; -fx-cursor: hand; -fx-font-size: 13;"));
        b.setOnMouseExited(
                e -> b.setStyle("-fx-background-color: #c9a84c; -fx-text-fill: #111; -fx-font-weight: bold; " +
                        "-fx-background-radius: 7; -fx-padding: 9 0; -fx-cursor: hand; -fx-font-size: 13;"));
        return b;
    }

    private Button outlineBtn(String text) {
        Button b = new Button(text);
        b.setPrefWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccc; " +
                "-fx-border-color: #555; -fx-border-radius: 7; -fx-background-radius: 7; -fx-padding: 10; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #fff; " +
                "-fx-border-color: #888; -fx-border-radius: 7; -fx-background-radius: 7; -fx-padding: 10; -fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccc; " +
                "-fx-border-color: #555; -fx-border-radius: 7; -fx-background-radius: 7; -fx-padding: 10; -fx-cursor: hand;"));
        return b;
    }

    // ── Button helpers for DIALOG (auto width, proper text) ──────
    private Button dialogGoldBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #c9a84c; -fx-text-fill: #111; -fx-font-weight: bold; " +
                "-fx-background-radius: 7; -fx-padding: 10 22; -fx-cursor: hand; -fx-font-size: 13;");
        b.setOnMouseEntered(
                e -> b.setStyle("-fx-background-color: #e0be6a; -fx-text-fill: #111; -fx-font-weight: bold; " +
                        "-fx-background-radius: 7; -fx-padding: 10 22; -fx-cursor: hand; -fx-font-size: 13;"));
        b.setOnMouseExited(
                e -> b.setStyle("-fx-background-color: #c9a84c; -fx-text-fill: #111; -fx-font-weight: bold; " +
                        "-fx-background-radius: 7; -fx-padding: 10 22; -fx-cursor: hand; -fx-font-size: 13;"));
        return b;
    }

    private Button dialogOutlineBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccc; " +
                "-fx-border-color: #555; -fx-border-radius: 7; -fx-background-radius: 7; -fx-padding: 10 22; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #fff; " +
                "-fx-border-color: #888; -fx-border-radius: 7; -fx-background-radius: 7; -fx-padding: 10 22; -fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccc; " +
                "-fx-border-color: #555; -fx-border-radius: 7; -fx-background-radius: 7; -fx-padding: 10 22; -fx-cursor: hand;"));
        return b;
    }

    private Label miniTag(String text) {
        Label l = new Label(text != null ? text : "—");
        l.setStyle("-fx-background-color: #333; -fx-text-fill: #999; -fx-font-size: 10; " +
                "-fx-background-radius: 4; -fx-padding: 2 7;");
        return l;
    }

    private ListCell<String> darkCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle("-fx-text-fill: #ffffff; -fx-background-color: #2a2a2a;");
            }
        };
    }
}
