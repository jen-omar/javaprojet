package com.marketplace.controllers;

import com.marketplace.models.Bid;
import com.marketplace.models.Order;
import com.marketplace.models.Product;
import com.marketplace.models.Review;
import com.marketplace.models.Wishlist;
import com.marketplace.services.BidService;
import com.marketplace.services.ChatbotService;
import com.marketplace.services.InvoiceService;
import com.marketplace.services.InvoiceService;
import com.marketplace.services.OrderService;
import com.marketplace.services.ProductService;
import com.marketplace.services.QuizService;
import com.marketplace.services.ReviewService;
import com.marketplace.services.VoiceService;
import com.marketplace.services.WishlistService;
import com.marketplace.services.PaymentServer;
import com.marketplace.services.StripeService;
import com.marketplace.util.SessionManager;

import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import java.util.Random;

import javafx.application.Platform;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

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
    private final InvoiceService invoiceService = new InvoiceService();
    private final VoiceService voiceService = new VoiceService();
    private final QuizService quizService = new QuizService();
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
        headerBox.setPadding(new Insets(35, 40, 25, 40));
        headerBox.setStyle("-fx-background-color: linear-gradient(to bottom, #222, #1a1a1a);");

        Label title = new Label("CATALOGUE D'ART");
        title.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 26; -fx-font-weight: 900; -fx-font-family: 'Segoe UI Black';");
        Label sub = new Label("Explorez la collection Mythoria — Artistes : " + session.getName());
        sub.setStyle("-fx-text-fill: #888; -fx-font-size: 13; -fx-letter-spacing: 1px;");
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

        // --- Voice Search Button ---
        Button voiceBtn = new Button("🎤");
        voiceBtn.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #c9a84c; -fx-font-size: 16; " +
                "-fx-border-color: #444; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        voiceBtn.setTooltip(new Tooltip("Recherche Vocale"));
        voiceBtn.setOnAction(e -> {
            voiceBtn.setStyle("-fx-background-color: #3a1a1a; -fx-text-fill: #ff6b6b; -fx-border-color: #ff6b6b;");
            voiceBtn.setText("⏳");
            voiceService.listenAsync().thenAccept(text -> Platform.runLater(() -> {
                voiceBtn.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #c9a84c; -fx-border-color: #444;");
                voiceBtn.setText("🎤");
                if (text != null && !text.equals("UNKNOWN") && !text.equals("TIMEOUT") && !text.startsWith("ERROR")) {
                    searchField.setText(text);
                    refreshCards();
                } else if (text != null && text.startsWith("ERROR")) {
                    System.err.println("Voice Error: " + text);
                }
            }));
        });

        HBox toolbar = new HBox(12, searchField, voiceBtn, filterType, sortOrder);
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
        Button chatBtn = new Button("MYTHORIA");
        chatBtn.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #c9a84c, #8e732a); " +
                "-fx-text-fill: #1a1a1a; -fx-font-weight: 900; -fx-font-size: 13; " +
                "-fx-background-radius: 25; -fx-cursor: hand; -fx-padding: 12 25; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 5); " +
                "-fx-font-family: 'Segoe UI Black';");
        
        StackPane.setAlignment(chatBtn, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatBtn, new Insets(0, 30, 30, 0));

        // Chat Window
        VBox chatWindow = new VBox(0);
        chatWindow.setMaxSize(380, 520);
        chatWindow.setStyle(
                "-fx-background-color: rgba(26, 26, 26, 0.95); " +
                "-fx-border-color: rgba(201, 168, 76, 0.3); " +
                "-fx-border-width: 1; -fx-border-radius: 20; " +
                "-fx-background-radius: 20; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 20, 0, 0, 10);");
        StackPane.setAlignment(chatWindow, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatWindow, new Insets(0, 30, 110, 0));
        chatWindow.setVisible(false);

        // Header
        HBox chatHeader = new HBox();
        chatHeader.setAlignment(Pos.CENTER_LEFT);
        chatHeader.setPadding(new Insets(15, 20, 15, 20));
        chatHeader.setStyle("-fx-background-color: linear-gradient(to right, #c9a84c, #8e732a); -fx-background-radius: 19 19 0 0;");
        
        VBox titleContainer = new VBox(2);
        Label chatTitle = new Label("MYTHORIA ASSISTANT");
        chatTitle.setStyle("-fx-text-fill: #1a1a1a; -fx-font-weight: bold; -fx-font-size: 15; -fx-font-family: 'Segoe UI';");
        Label statusLabel = new Label("En ligne");
        statusLabel.setStyle("-fx-text-fill: #1a3a1a; -fx-font-size: 10; -fx-font-weight: bold;");
        titleContainer.getChildren().addAll(chatTitle, statusLabel);

        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);
        Button closeChatBtn = new Button("✕");
        closeChatBtn.setStyle(
                "-fx-background-color: rgba(0,0,0,0.1); -fx-text-fill: #1a1a1a; -fx-cursor: hand; " +
                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 5 10;");
        closeChatBtn.setOnAction(e -> chatWindow.setVisible(false));
        chatHeader.getChildren().addAll(titleContainer, spacerHeader, closeChatBtn);

        // Messages area
        VBox messagesBox = new VBox(15);
        messagesBox.setPadding(new Insets(20));
        messagesBox.setStyle("-fx-background-color: transparent;");
        ScrollPane chatScroll = new ScrollPane(messagesBox);
        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(chatScroll, Priority.ALWAYS);
        chatScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Input area
        HBox inputArea = new HBox(10);
        inputArea.setAlignment(Pos.CENTER);
        inputArea.setPadding(new Insets(15, 20, 20, 20));
        inputArea.setStyle("-fx-background-color: transparent;");
        
        TextField chatInput = new TextField();
        chatInput.setPromptText("Écrivez votre message...");
        chatInput.setStyle(
                "-fx-background-color: #2a2a2a; -fx-text-fill: #fff; -fx-prompt-text-fill: #666; " +
                "-fx-background-radius: 25; -fx-padding: 12 20; -fx-border-color: #444; -fx-border-radius: 25;");
        HBox.setHgrow(chatInput, Priority.ALWAYS);
        
        Button sendBtn = new Button("➤");
        sendBtn.setPrefSize(45, 45);
        sendBtn.setMinSize(45, 45);
        sendBtn.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #c9a84c, #8e732a); " +
                "-fx-text-fill: #1a1a1a; -fx-background-radius: 25; -fx-cursor: hand; " +
                "-fx-font-weight: bold; -fx-font-size: 16;");

        // Add intro message
        Label intro = new Label("Bonjour " + session.getName() + " ! Comment puis-je vous aider aujourd'hui ?");
        intro.setWrapText(true);
        intro.setMaxWidth(260);
        intro.setStyle(
                "-fx-background-color: #2a2a2a; -fx-text-fill: #ddd; -fx-padding: 12 16; " +
                "-fx-background-radius: 0 20 20 20; -fx-font-size: 13; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);");
        HBox introRow = new HBox(intro);
        introRow.setAlignment(Pos.CENTER_LEFT);
        messagesBox.getChildren().add(introRow);

        ChatbotService botService = new ChatbotService();

        Runnable sendMessage = () -> {
            String text = chatInput.getText().trim();
            if (text.isEmpty())
                return;

            // User msg
            Label uMsg = new Label(text);
            uMsg.setWrapText(true);
            uMsg.setMaxWidth(260);
            uMsg.setStyle(
                    "-fx-background-color: #c9a84c; -fx-text-fill: #1a1a1a; -fx-padding: 12 16; " +
                    "-fx-background-radius: 20 20 0 20; -fx-font-size: 13; -fx-font-weight: 500; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);");
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

                Platform.runLater(() -> {
                    messagesBox.getChildren().remove(tRow);
                    
                    VBox bContainer = new VBox(5);
                    bContainer.setAlignment(Pos.CENTER_LEFT);
                    
                    Label nameTag = new Label("Mythoria Assistant");
                    nameTag.setStyle("-fx-text-fill: #8e732a; -fx-font-size: 9; -fx-font-weight: bold; -fx-padding: 0 0 0 5;");
                    
                    Label bMsg = new Label(reply);
                    bMsg.setWrapText(true);
                    bMsg.setMaxWidth(280);
                    bMsg.setStyle(
                            "-fx-background-color: #2a2a2a; -fx-text-fill: #eee; -fx-padding: 14 18; " +
                            "-fx-background-radius: 0 20 20 20; -fx-font-size: 13.5; -fx-line-spacing: 2; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 8, 0, 0, 4); " +
                            "-fx-border-color: rgba(201, 168, 76, 0.1); -fx-border-radius: 0 20 20 20;");
                    
                    bContainer.getChildren().addAll(nameTag, bMsg);
                    HBox bRow = new HBox(bContainer);
                    bRow.setAlignment(Pos.CENTER_LEFT);
                    messagesBox.getChildren().add(bRow);
                    chatScroll.setVvalue(1.0); // scroll to bottom
                });
            }).start();
        };

        sendBtn.setOnAction(e -> sendMessage.run());
        chatInput.setOnAction(e -> sendMessage.run());

        inputArea.getChildren().addAll(chatInput, sendBtn);
        chatWindow.getChildren().addAll(chatHeader, chatScroll, inputArea);

        chatBtn.setOnAction(e -> chatWindow.setVisible(!chatWindow.isVisible()));

        stack.getChildren().addAll(chatBtn, chatWindow);

        // --- Gamified Quiz Logic ---
        // if (!quizService.hasAlreadyWon()) {
            VBox quizCard = createQuizUI(stack);
            stack.getChildren().add(quizCard);
            StackPane.setAlignment(quizCard, Pos.BOTTOM_LEFT);
            StackPane.setMargin(quizCard, new Insets(0, 0, 30, 30));
        // }

        return stack;
    }

    private VBox createQuizUI(StackPane mainContainer) {
        VBox quizCard = new VBox(10);
        quizCard.setMaxSize(300, 200);
        quizCard.setPadding(new Insets(20));
        quizCard.setStyle(
                "-fx-background-color: rgba(30, 30, 30, 0.9); -fx-background-radius: 20; " +
                "-fx-border-color: rgba(201, 168, 76, 0.5); -fx-border-width: 1.5; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 15, 0, 0, 10);");

        Label quizHeader = new Label("🎨 QUIZ POUR " + session.getName().toUpperCase());
        quizHeader.setStyle("-fx-text-fill: #c9a84c; -fx-font-weight: bold; -fx-font-size: 14;");

        Label question = new Label("Quel peintre néerlandais est célèbre pour sa 'Nuit Étoilée' ?");
        question.setWrapText(true);
        question.setStyle("-fx-text-fill: white; -fx-font-size: 12;");

        TextField quizInput = new TextField();
        quizInput.setPromptText("Votre réponse...");
        quizInput.setStyle("-fx-background-color: #222; -fx-text-fill: white; -fx-border-color: #444; -fx-border-radius: 5;");

        Button submitQuiz = new Button("Vérifier");
        submitQuiz.setStyle("-fx-background-color: #c9a84c; -fx-text-fill: black; -fx-font-weight: bold; -fx-cursor: hand;");
        submitQuiz.setPrefWidth(Double.MAX_VALUE);

        submitQuiz.setOnAction(e -> {
            String ans = quizInput.getText().trim();
            if ("van gogh".equalsIgnoreCase(ans)) {
                triggerConfetti(mainContainer);
                quizService.markAsWon();
                
                quizCard.getChildren().clear();
                Label success = new Label("BRAVO ! 🎉");
                success.setStyle("-fx-text-fill: #7ec97e; -fx-font-weight: bold; -fx-font-size: 18;");
                Label code = new Label("Code Promo : MYTHORIA2026");
                code.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
                Button close = new Button("Fermer");
                close.setOnAction(ev -> mainContainer.getChildren().remove(quizCard));
                quizCard.getChildren().addAll(success, code, close);
            } else {
                quizInput.setStyle("-fx-background-color: #222; -fx-text-fill: white; -fx-border-color: #ff6b6b; -fx-border-radius: 5;");
            }
        });

        quizCard.getChildren().addAll(quizHeader, question, quizInput, submitQuiz);
        return quizCard;
    }

    private void triggerConfetti(StackPane container) {
        Random rnd = new Random();
        for (int i = 0; i < 50; i++) {
            javafx.scene.shape.Rectangle c = new javafx.scene.shape.Rectangle(rnd.nextInt(5, 10), rnd.nextInt(5, 10));
            c.setFill(Color.hsb(rnd.nextDouble() * 360, 0.8, 0.9));
            c.setTranslateX(rnd.nextInt((int) container.getWidth()));
            c.setTranslateY(-20);
            container.getChildren().add(c);

            TranslateTransition tt = new TranslateTransition(Duration.seconds(2 + rnd.nextDouble() * 2), c);
            tt.setToY(container.getHeight() + 20);
            tt.setToX(c.getTranslateX() + rnd.nextInt(-50, 50));
            
            FadeTransition ft = new FadeTransition(Duration.seconds(2 + rnd.nextDouble() * 2), c);
            ft.setToValue(0);

            ParallelTransition pt = new ParallelTransition(tt, ft);
            pt.setOnFinished(e -> container.getChildren().remove(c));
            pt.play();
        }
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
        card.setPrefWidth(280);
        card.setStyle(
                "-fx-background-color: #222; -fx-background-radius: 18; " +
                "-fx-border-color: #333; -fx-border-radius: 18; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 5);");

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #282828; -fx-background-radius: 18; " +
                "-fx-border-color: #c0c0c0; -fx-border-radius: 18; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(192,192,192,0.2), 15, 0, 0, 8);"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: #222; -fx-background-radius: 18; " +
                "-fx-border-color: #333; -fx-border-radius: 18; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 5);"));

        // Image
        String rawUrl = p.getImageUrl();
        Node imgNode;
        if (rawUrl != null && !rawUrl.isBlank()) {
            try {
                String url = rawUrl.startsWith("http") || rawUrl.startsWith("file:") ? rawUrl : "http://localhost" + rawUrl;
                Image img = new Image(url, 280, 160, false, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(280);
                iv.setFitHeight(160);
                iv.setPreserveRatio(false);
                iv.setClip(new javafx.scene.shape.Rectangle(280, 160) {{ setArcWidth(36); setArcHeight(36); }});
                iv.setCursor(Cursor.HAND);
                iv.setOnMouseClicked(e -> show3DMuseumView(p));
                imgNode = iv;
            } catch (Exception ignored) { imgNode = new Region(); }
        } else {
            StackPane placeholder = new StackPane(new Label("🖼  Pas de photo"));
            placeholder.setPrefSize(280, 160);
            placeholder.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 18 18 0 0;");
            placeholder.setCursor(Cursor.HAND);
            placeholder.setOnMouseClicked(e -> show3DMuseumView(p));
            imgNode = placeholder;
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(18, 20, 20, 20));

        Label name = new Label(p.getName());
        name.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: 900; -fx-font-size: 16; -fx-font-family: 'Segoe UI';");
        name.setWrapText(true);

        Label artist = new Label("par " + (p.getArtistName() != null ? p.getArtistName() : "—"));
        artist.setStyle("-fx-text-fill: #888; -fx-font-size: 12; -fx-font-style: italic;");

        Label price = new Label(p.getPrice() != null ? p.getPrice().toPlainString() + " €" : "—");
        price.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 20; -fx-font-weight: 900;");

        HBox tags = new HBox(8);
        tags.getChildren().addAll(miniTag(p.getType()), miniTag(p.getSaleType()));

        Button actionBtn = new Button("auction".equalsIgnoreCase(p.getSaleType()) ? "⚡ ENCHÉRIR" : "🛒 ACHETER");
        actionBtn.setPrefWidth(Double.MAX_VALUE);
        actionBtn.setStyle("-fx-background-color: #c0c0c0; -fx-text-fill: #1a1a1a; -fx-font-weight: bold; " +
                "-fx-background-radius: 10; -fx-padding: 10 0; -fx-cursor: hand; -fx-font-size: 13;");
        actionBtn.setOnAction(e -> {
            if ("auction".equalsIgnoreCase(p.getSaleType())) showBidDialog(p);
            else showBuyDialog(p);
        });

        Button wishBtn = new Button("♥");
        wishBtn.setPrefSize(40, 40);
        wishBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; -fx-border-color: #444; -fx-border-radius: 10; -fx-cursor: hand;");
        wishBtn.setOnAction(e -> {
            Wishlist w = new Wishlist();
            w.setClientName(session.getName());
            w.setProductId(p.getId());
            wishlistService.add(w);
            wishBtn.setStyle("-fx-background-color: #333; -fx-text-fill: #7ec97e; -fx-border-color: #7ec97e; -fx-border-radius: 10;");
        });
        
        Button avisBtn = new Button("★");
        avisBtn.setPrefSize(40, 40);
        avisBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; -fx-border-color: #444; -fx-border-radius: 10; -fx-cursor: hand;");
        avisBtn.setOnAction(e -> showAvisDialog(p));

        HBox bottomRow = new HBox(12, wishBtn, avisBtn);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        content.getChildren().addAll(name, artist, price, tags, spacer, actionBtn, bottomRow);
        card.getChildren().addAll(imgNode, content);
        return card;
    }

    // ── Bid dialog ───────────────────────────────────────────────
    private void showBidDialog(Product p) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Enchérir sur : " + p.getName());
        dialog.setWidth(460);
        dialog.setResizable(false);

        VBox layout = new VBox(20);
        layout.setPadding(new Insets(35, 40, 35, 40));
        layout.setStyle("-fx-background-color: #1a1a1a;");

        Label title = new Label("HISTORIQUE DES ENCHÈRES");
        title.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 18; -fx-font-weight: 900; -fx-font-family: 'Segoe UI Black';");

        VBox historyBox = new VBox(10);
        historyBox.setPadding(new Insets(15));
        historyBox.setStyle("-fx-background-color: #222; -fx-border-color: #333; -fx-border-radius: 12; -fx-background-radius: 12;");
        historyBox.setPrefHeight(180);

        List<Bid> bids = bidService.getByProductId(p.getId());
        BigDecimal maxBid = p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;

        if (bids.isEmpty()) {
            Label noBids = new Label("Aucune enchère pour le moment. Prix : " + maxBid.toPlainString() + " €");
            noBids.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
            historyBox.getChildren().add(noBids);
        } else {
            for (int i = 0; i < bids.size(); i++) {
                Bid b = bids.get(i);
                if (i == 0 && b.getAmount().compareTo(maxBid) > 0) maxBid = b.getAmount();

                HBox bidRow = new HBox(10);
                bidRow.setAlignment(Pos.CENTER_LEFT);
                Label bName = new Label(b.getBidderName());
                bName.setStyle("-fx-text-fill: #ddd; -fx-font-weight: bold; -fx-font-size: 13;");
                bName.setPrefWidth(120);
                Label bAmt = new Label(b.getAmount().toPlainString() + " €");
                bAmt.setStyle("-fx-text-fill: #c0c0c0; -fx-font-weight: bold; -fx-font-size: 14;");
                bidRow.getChildren().addAll(bName, bAmt);
                historyBox.getChildren().add(bidRow);
            }
        }

        ScrollPane scroll = new ScrollPane(historyBox);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(160);
        scroll.setStyle("-fx-background: #222; -fx-background-color: #222; -fx-border-color: transparent;");

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
                refreshCards();
            } catch (Exception ex) {
                setErr(tfAmount);
                errLbl.setText("Montant invalide.");
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
        dialog.setWidth(440);
        dialog.setResizable(false);

        VBox layout = new VBox(22);
        layout.setPadding(new Insets(35, 40, 35, 40));
        layout.setStyle("-fx-background-color: #1a1a1a;");

        Label title = new Label("CONFIRMER L'ACHAT");
        title.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 18; -fx-font-weight: 900; -fx-font-family: 'Segoe UI Black';");

        VBox summaryBox = new VBox(10);
        summaryBox.setPadding(new Insets(18));
        summaryBox.setStyle("-fx-background-color: #222; -fx-background-radius: 12; -fx-border-color: #333; -fx-border-radius: 12; -fx-border-width: 1;");
        Label pName = new Label(p.getName());
        pName.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 15; -fx-font-weight: bold;");
        Label pPrice = new Label("Prix : " + (p.getPrice() != null ? p.getPrice().toPlainString() + " €" : "—"));
        pPrice.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 18; -fx-font-weight: 900;");
        summaryBox.getChildren().addAll(pName, pPrice);

        Label buyerLabel = new Label("Votre nom *");
        buyerLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
        TextField tfBuyer = clientField(session.getName());

        Label promoLabel = new Label("Code Promo (optionnel)");
        promoLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
        TextField tfPromo = clientField("");
        tfPromo.setPromptText("Ex: MYTHORIA2026");

        Label errLbl = new Label("");
        errLbl.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11;");

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

            BigDecimal finalPrice = p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;
            String promo = tfPromo.getText().trim();
            if ("MYTHORIA2026".equalsIgnoreCase(promo)) {
                // Apply 20% discount
                finalPrice = finalPrice.multiply(new BigDecimal("0.8"));
                errLbl.setText("✓ Code Promo appliqué ! -20%");
                errLbl.setStyle("-fx-text-fill: #7ec97e;");
            }

            try {
                final BigDecimal priceToPay = finalPrice;
                PaymentServer paymentServer = new PaymentServer(
                    () -> Platform.runLater(() -> {
                        Order order = new Order();
                        order.setBuyerName(buyer);
                        order.setPrice(priceToPay);
                        order.setOrderType(p.getSaleType() != null ? p.getSaleType() : "fixed");
                        order.setProductId(p.getId());
                        orderService.add(order);
                        productService.markAsSold(p.getId());
                        
                        // Generate Invoice & QR Code
                        invoiceService.generateInvoice(order, p);
                        
                        dialog.close();
                        showSuccessDialog(p, buyer);
                        refreshCards();
                    }),
                    () -> Platform.runLater(() -> {
                        errLbl.setText("Paiement annulé.");
                        errLbl.setStyle("-fx-text-fill: #ff6b6b;");
                    })
                );
                paymentServer.start();
                StripeService stripeService = new StripeService();
                String url = stripeService.createCheckoutSession(p, "http://localhost:" + paymentServer.getPort() + "/success", "http://localhost:" + paymentServer.getPort() + "/cancel");
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception ex) {
                errLbl.setText("Erreur Stripe.");
            }
        });

        HBox btnRow = new HBox(12, confirmBtn, cancelBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(confirmBtn, Priority.ALWAYS);
        HBox.setHgrow(cancelBtn, Priority.ALWAYS);

        layout.getChildren().addAll(title, summaryBox, buyerLabel, tfBuyer, promoLabel, tfPromo, errLbl, btnRow);
        dialog.setScene(new Scene(layout));
        dialog.show();
    }

    private void showSuccessDialog(Product p, String buyer) {
        Stage success = new Stage();
        success.initModality(Modality.APPLICATION_MODAL);
        success.setTitle("Achat confirmé !");
        success.setWidth(350);
        VBox layout = new VBox(16);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #1a1a1a;");
        Label msg = new Label("Merci " + buyer + " ! Votre achat est confirmé.");
        msg.setStyle("-fx-text-fill: #aaa; -fx-font-size: 13;");
        Button ok = dialogGoldBtn("Fermer");
        ok.setOnAction(e -> success.close());
        layout.getChildren().addAll(msg, ok);
        success.setScene(new Scene(layout));
        success.show();
    }

    private void showAvisDialog(Product p) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Laisser un avis");
        dialog.setWidth(400);
        VBox layout = new VBox(16);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: #1a1a1a;");

        Label title = new Label("★ Laisser un avis");
        title.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 20; -fx-font-weight: bold;");
        
        Label ratingLbl = new Label("Votre note :");
        ratingLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 13;");

        HBox starRow = new HBox(5);
        starRow.setAlignment(Pos.CENTER_LEFT);
        final int[] selectedRating = {0};
        Button[] stars = new Button[5];

        for (int i = 0; i < 5; i++) {
            final int val = i + 1;
            Button star = new Button("☆");
            star.setStyle("-fx-background-color: transparent; -fx-text-fill: #555; -fx-font-size: 26; -fx-padding: 0 4; -fx-cursor: hand;");
            star.setOnAction(e -> {
                selectedRating[0] = val;
                for (int j = 0; j < 5; j++) {
                    boolean filled = (j < val);
                    stars[j].setText(filled ? "★" : "☆");
                    stars[j].setStyle("-fx-background-color: transparent; -fx-text-fill: " + (filled ? "#c9a84c" : "#555") + "; -fx-font-size: 26; -fx-padding: 0 4; -fx-cursor: hand;");
                }
            });
            stars[i] = star;
            starRow.getChildren().add(star);
        }

        TextArea taComment = new TextArea();
        taComment.setPromptText("Partagez votre avis sur cette oeuvre...");
        taComment.setPrefRowCount(4);
        taComment.setWrapText(true);
        taComment.setStyle("-fx-control-inner-background: #2a2a2a; -fx-text-fill: #fff; -fx-border-color: #444; -fx-border-radius: 5;");
        
        Label errLbl = new Label("");
        errLbl.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11;");

        Button submit = dialogGoldBtn("Envoyer l'avis");
        submit.setPrefWidth(Double.MAX_VALUE);
        submit.setOnAction(e -> {
            if (selectedRating[0] == 0) {
                errLbl.setText("Veuillez sélectionner une note.");
                return;
            }
            Review r = new Review();
            r.setReviewerName(session.getName());
            r.setRating(selectedRating[0]);
            r.setComment(taComment.getText().trim());
            r.setProductId(p.getId());
            reviewService.add(r);
            dialog.close();
            
            // Feedback
            Stage ok = new Stage();
            VBox okBox = new VBox(15);
            okBox.setPadding(new Insets(20));
            okBox.setAlignment(Pos.CENTER);
            okBox.setStyle("-fx-background-color: #1a1a1a;");
            Label okLbl = new Label("Merci pour votre avis !");
            okLbl.setStyle("-fx-text-fill: #7ec97e; -fx-font-weight: bold;");
            Button close = dialogGoldBtn("Fermer");
            close.setOnAction(ev -> ok.close());
            okBox.getChildren().addAll(okLbl, close);
            ok.setScene(new Scene(okBox));
            ok.show();
        });
        
        layout.getChildren().addAll(title, ratingLbl, starRow, taComment, errLbl, submit);
        dialog.setScene(new Scene(layout));
        dialog.show();
    }

    // ── 3D Showroom (Native JavaFX 3D) ───────────────────────────
    private void show3DMuseumView(Product product) {
        Stage stage = new Stage();
        stage.setTitle("Galerie 3D — " + product.getName());
        stage.initModality(Modality.APPLICATION_MODAL);

        Group root3D = new Group();
        Group room = new Group(); 
        
        PhongMaterial wallMat = new PhongMaterial(Color.web("#2a2c30"));
        PhongMaterial floorMat = new PhongMaterial(Color.web("#111111"));
        PhongMaterial goldFrameMat = new PhongMaterial(Color.web("#d4af37"));
        goldFrameMat.setSpecularColor(Color.WHITE);
        
        Box floor = new Box(1500, 2, 1500);
        floor.setMaterial(floorMat);
        floor.setTranslateY(300);

        Box ceiling = new Box(1500, 2, 1500);
        ceiling.setMaterial(new PhongMaterial(Color.web("#0a0a0a")));
        ceiling.setTranslateY(-400);

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
        
        Box frame = new Box(340, 260, 20);
        frame.setMaterial(goldFrameMat);
        frame.setTranslateZ(580);
        frame.setTranslateY(-60);

        Box canvas = new Box(300, 220, 5);
        PhongMaterial canvasMat = new PhongMaterial();
        String rawUrl = product.getImageUrl();
        if (rawUrl != null && !rawUrl.isBlank()) {
            try {
                String urlStr = rawUrl.startsWith("http") || rawUrl.startsWith("file:") ? rawUrl : "http://localhost" + rawUrl;
                canvasMat.setDiffuseMap(new Image(urlStr, true));
            } catch (Exception e) {
                canvasMat.setDiffuseColor(Color.DARKGRAY);
            }
        } else {
            canvasMat.setDiffuseColor(Color.DARKGRAY);
        }
        canvas.setMaterial(canvasMat);
        canvas.setTranslateZ(568);
        canvas.setTranslateY(-60);

        AmbientLight ambient = new AmbientLight(Color.rgb(100, 100, 120, 0.6));
        PointLight pointLight = new PointLight(Color.web("#fff5e6"));
        pointLight.setTranslateZ(250);
        pointLight.setTranslateY(-200);

        room.getChildren().addAll(floor, ceiling, backWall, leftWall, rightWall, frame, canvas);
        root3D.getChildren().addAll(room, ambient, pointLight);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(5000.0);
        camera.setTranslateZ(-1000);
        camera.setTranslateY(-100);

        SubScene subScene = new SubScene(root3D, 1100, 750, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);
        subScene.setFill(Color.BLACK);

        Rotate rotateX = new Rotate(-10, Rotate.X_AXIS);
        Rotate rotateY = new Rotate(20, Rotate.Y_AXIS);
        room.getTransforms().addAll(rotateX, rotateY);

        subScene.setOnMouseDragged(e -> {
            rotateY.setAngle(rotateY.getAngle() + (e.getSceneX() > 550 ? 1 : -1) * 2);
            rotateX.setAngle(rotateX.getAngle() + (e.getSceneY() > 375 ? -1 : 1) * 2);
        });

        VBox info = new VBox(10);
        info.setPadding(new Insets(30));
        info.setStyle("-fx-background-color: rgba(20,20,20,0.85); -fx-background-radius: 0 20 0 0;");
        Label lTitle = new Label(product.getName());
        lTitle.setStyle("-fx-text-fill: white; -fx-font-size: 26; -fx-font-weight: bold;");
        Label lArt = new Label("Oeuvre de " + (product.getArtistName()!=null?product.getArtistName():"Anonyme"));
        lArt.setStyle("-fx-text-fill: #d4af37; -fx-font-size: 16; -fx-font-style: italic;");
        Button backBtn = new Button("← Sortir de la galerie");
        backBtn.setStyle("-fx-background-color: #d4af37; -fx-text-fill: black; -fx-font-weight: bold; -fx-cursor: hand;");
        backBtn.setOnAction(e -> stage.close());
        info.getChildren().addAll(lTitle, lArt, backBtn);

        StackPane root = new StackPane(subScene, info);
        StackPane.setAlignment(info, Pos.BOTTOM_LEFT);
        
        Scene scene = new Scene(root, 1100, 750);
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case W, UP -> camera.setTranslateZ(camera.getTranslateZ() + 20);
                case S, DOWN -> camera.setTranslateZ(camera.getTranslateZ() - 20);
                case A, LEFT -> camera.setTranslateX(camera.getTranslateX() - 20);
                case D, RIGHT -> camera.setTranslateX(camera.getTranslateX() + 20);
            }
        });

        stage.setScene(scene);
        stage.show();
    }

    private TextField clientField(String value) {
        TextField tf = new TextField(value);
        tf.setStyle("-fx-control-inner-background: #2a2a2a; -fx-text-fill: #fff; -fx-border-color: #444; -fx-padding: 8;");
        return tf;
    }

    private void setErr(TextField tf) {
        tf.setStyle(tf.getStyle() + "-fx-border-color: #ff6b6b;");
    }

    private Button goldBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #c9a84c; -fx-text-fill: #111; -fx-font-weight: bold; -fx-padding: 10 20;");
        return b;
    }

    private Button dialogGoldBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #c9a84c; -fx-text-fill: #111; -fx-font-weight: bold; -fx-padding: 10 20;");
        return b;
    }

    private Button dialogOutlineBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccc; -fx-border-color: #555; -fx-padding: 10 20;");
        return b;
    }

    private Label miniTag(String text) {
        Label l = new Label(text != null ? text : "—");
        l.setStyle("-fx-background-color: #333; -fx-text-fill: #999; -fx-font-size: 10; -fx-padding: 2 6;");
        return l;
    }

    private ListCell<String> darkCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle("-fx-text-fill: #fff; -fx-background-color: #2a2a2a;");
            }
        };
    }
}
