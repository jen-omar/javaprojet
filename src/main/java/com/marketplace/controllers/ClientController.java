package com.marketplace.controllers;

import com.marketplace.models.Order;
import com.marketplace.models.Product;
import com.marketplace.models.Review;
import com.marketplace.models.Wishlist;
import com.marketplace.services.OrderService;
import com.marketplace.services.ProductService;
import com.marketplace.services.ReviewService;
import com.marketplace.services.WishlistService;
import com.marketplace.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import java.util.stream.Collectors;

/**
 * ClientController — vue Client:
 *   • Affiche tous les produits "available" sous forme de catalogue.
 *   • Permet au client d'acheter un produit (crée une Order).
 *   • Barre de recherche + filtre par type.
 */
public class ClientController {

    private final ProductService  productService  = new ProductService();
    private final OrderService    orderService    = new OrderService();
    private final ReviewService   reviewService   = new ReviewService();
    private final WishlistService wishlistService = new WishlistService();
    private final SessionManager  session         = SessionManager.getInstance();

    private FlowPane cards;
    private TextField searchField;
    private ComboBox<String> filterType;
    private ComboBox<String> sortOrder;  // tri par prix

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
        return scroll;
    }

    // ── Refresh with search + filter + sort ─────────────────────
    private void refreshCards() {
        cards.getChildren().clear();
        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        String type  = filterType  != null ? filterType.getValue()  : "Tous les types";
        String sort  = sortOrder   != null ? sortOrder.getValue()   : "Prix : défaut";

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
        for (Product p : products) cards.getChildren().add(buildCard(p));
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
                "-fx-border-color: #c9a84c; -fx-border-radius: 12; -fx-border-width: 1;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: #222; -fx-background-radius: 12; " +
                "-fx-border-color: #2e2e2e; -fx-border-radius: 12; -fx-border-width: 1;"));

        // Image
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
                img.errorProperty().addListener((obs, old, err) -> {
                    if (err) card.getChildren().remove(iv);
                });
                card.getChildren().add(iv);
            } catch (Exception ignored) {}
        }

        // Content
        VBox content = new VBox(8);
        content.setPadding(new Insets(14, 16, 16, 16));

        Label name = new Label(p.getName());
        name.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 14;");
        name.setWrapText(true);

        Label artist = new Label("par " + (p.getArtistName() != null ? p.getArtistName() : "—"));
        artist.setStyle("-fx-text-fill: #777; -fx-font-size: 11;");

        Label price = new Label(p.getPrice() != null ? p.getPrice().toPlainString() + " €" : "—");
        price.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 18; -fx-font-weight: bold;");

        String d = p.getDescription() != null && !p.getDescription().isBlank()
                ? p.getDescription() : "Aucune description.";
        Label desc = new Label(d.length() > 70 ? d.substring(0, 70) + "…" : d);
        desc.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");
        desc.setWrapText(true);

        HBox tags = new HBox(6);
        tags.getChildren().addAll(miniTag(p.getType()), miniTag(p.getSaleType()));

        // Buy button
        Button buyBtn = new Button("🛒  Acheter");
        buyBtn.setPrefWidth(Double.MAX_VALUE);
        buyBtn.setStyle(
                "-fx-background-color: #c9a84c; -fx-text-fill: #111; -fx-font-weight: bold; " +
                "-fx-background-radius: 7; -fx-padding: 9 0; -fx-cursor: hand; -fx-font-size: 13;");
        buyBtn.setOnMouseEntered(e -> buyBtn.setStyle(
                "-fx-background-color: #e0be6a; -fx-text-fill: #111; -fx-font-weight: bold; " +
                "-fx-background-radius: 7; -fx-padding: 9 0; -fx-cursor: hand; -fx-font-size: 13;"));
        buyBtn.setOnMouseExited(e -> buyBtn.setStyle(
                "-fx-background-color: #c9a84c; -fx-text-fill: #111; -fx-font-weight: bold; " +
                "-fx-background-radius: 7; -fx-padding: 9 0; -fx-cursor: hand; -fx-font-size: 13;"));
        buyBtn.setOnAction(e -> showBuyDialog(p));

        // ── ♥ Wishlist button ────────────────────────────────────
        Button wishBtn = new Button("♥  Souhait");
        boolean alreadyWished = wishlistService.isAlreadyInWishlist(session.getName(), p.getId());
        if (alreadyWished) {
            wishBtn.setText("♥  Ajouté");
            wishBtn.setStyle("-fx-background-color: #3a2a1a; -fx-text-fill: #c9a84c; " +
                    "-fx-background-radius: 7; -fx-padding: 7 12; -fx-cursor: default; -fx-font-size: 12; " +
                    "-fx-border-color: #c9a84c; -fx-border-radius: 7; -fx-border-width: 1;");
            wishBtn.setDisable(true);
        } else {
            wishBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #c9a84c; " +
                    "-fx-border-color: #c9a84c; -fx-border-radius: 7; -fx-background-radius: 7; " +
                    "-fx-padding: 7 12; -fx-cursor: hand; -fx-font-size: 12;");
            wishBtn.setOnMouseEntered(e -> wishBtn.setStyle(
                    "-fx-background-color: #2a1e0e; -fx-text-fill: #e0be6a; " +
                    "-fx-border-color: #e0be6a; -fx-border-radius: 7; -fx-background-radius: 7; " +
                    "-fx-padding: 7 12; -fx-cursor: hand; -fx-font-size: 12;"));
            wishBtn.setOnMouseExited(e -> wishBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #c9a84c; " +
                    "-fx-border-color: #c9a84c; -fx-border-radius: 7; -fx-background-radius: 7; " +
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
        content.getChildren().addAll(name, artist, price, desc, tags, spacer, buyBtn, secondRow);
        card.getChildren().add(content);
        return card;
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
        title.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 18; -fx-font-weight: bold;");

        // Product summary box
        VBox summaryBox = new VBox(8);
        summaryBox.setPadding(new Insets(16));
        summaryBox.setStyle(
                "-fx-background-color: #2a2a2a; -fx-background-radius: 10; " +
                "-fx-border-color: #333; -fx-border-radius: 10; -fx-border-width: 1;");

        Label pName  = new Label(p.getName());
        pName.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14; -fx-font-weight: bold;");
        pName.setWrapText(true);
        Label pArtist = new Label("Artiste : " + (p.getArtistName() != null ? p.getArtistName() : "—"));
        pArtist.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
        Label pPrice = new Label("Prix : " + (p.getPrice() != null ? p.getPrice().toPlainString() + " €" : "—"));
        pPrice.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 15; -fx-font-weight: bold;");
        Label pType  = new Label("Type : " + p.getType() + "   ·   " + p.getSaleType());
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
        Button cancelBtn  = dialogOutlineBtn("✕  Annuler");
        cancelBtn.setOnAction(e -> dialog.close());

        confirmBtn.setOnAction(e -> {
            String buyer = tfBuyer.getText().trim();
            if (buyer.isBlank()) {
                setErr(tfBuyer); errLbl.setText("Votre nom est requis.");
                return;
            }
            if (buyer.length() < 2) {
                setErr(tfBuyer); errLbl.setText("Nom trop court (min 2 caractères).");
                return;
            }

            // Créer la commande
            Order order = new Order();
            order.setBuyerName(buyer);
            order.setPrice(p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO);
            order.setOrderType(p.getSaleType() != null ? p.getSaleType() : "fixed");
            order.setProductId(p.getId());
            orderService.add(order);

            // Marquer le produit comme vendu immédiatement
            productService.markAsSold(p.getId());

            dialog.close();
            showSuccessDialog(p, buyer);
            refreshCards(); // retire le produit du catalogue
        });

        HBox btnRow = new HBox(12, confirmBtn, cancelBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(confirmBtn, Priority.ALWAYS);
        HBox.setHgrow(cancelBtn,  Priority.ALWAYS);

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

        Label icon  = new Label("✅");
        icon.setStyle("-fx-font-size: 42;");

        Label msg1 = new Label("Commande confirmée !");
        msg1.setStyle("-fx-text-fill: #a8d5a2; -fx-font-size: 18; -fx-font-weight: bold;");

        Label msg2 = new Label("Merci " + buyer + " !\nVotre achat de « " + p.getName() + " »\na été enregistré avec succès.");
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

        final int[] selectedRating = {0};
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
                    ? tf.getStyle().replace("-fx-border-color: #444;", "-fx-border-color: #c9a84c; -fx-border-width: 1.5;")
                    : tf.getStyle()
                          .replace("-fx-border-color: #c9a84c; -fx-border-width: 1.5;", "-fx-border-color: #444;"));
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
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #e0be6a; -fx-text-fill: #111; -fx-font-weight: bold; " +
                "-fx-background-radius: 7; -fx-padding: 9 0; -fx-cursor: hand; -fx-font-size: 13;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: #c9a84c; -fx-text-fill: #111; -fx-font-weight: bold; " +
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
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #e0be6a; -fx-text-fill: #111; -fx-font-weight: bold; " +
                "-fx-background-radius: 7; -fx-padding: 10 22; -fx-cursor: hand; -fx-font-size: 13;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: #c9a84c; -fx-text-fill: #111; -fx-font-weight: bold; " +
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
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle("-fx-text-fill: #ffffff; -fx-background-color: #2a2a2a;");
            }
        };
    }
}
