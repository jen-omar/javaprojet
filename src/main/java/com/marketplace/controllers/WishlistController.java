package com.marketplace.controllers;

import com.marketplace.models.Order;
import com.marketplace.models.Product;
import com.marketplace.models.Wishlist;
import com.marketplace.services.OrderService;
import com.marketplace.services.ProductService;
import com.marketplace.services.WishlistService;
import com.marketplace.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** WishlistController — handles the Liste de Souhaits view. */
public class WishlistController {

    private final WishlistService service        = new WishlistService();
    private final ProductService  productService = new ProductService();
    private final OrderService    orderService   = new OrderService();
    private final SessionManager  session        = SessionManager.getInstance();
    private Map<Integer, Product> productMap     = new java.util.HashMap<>();

    public Node buildView() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1a1a1a;");

        Label title = new Label("LISTE DE SOUHAITS");
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 22; -fx-font-weight: bold;");
        String subText = session.isClient()
                ? "Vos produits favoris — achetez-les directement ici"
                : "Produits mis en favoris par les clients";
        Label subtitle = new Label(subText);
        subtitle.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");
        VBox titleBox = new VBox(2, title, subtitle);

        HBox headerRow = new HBox();
        headerRow.setPadding(new Insets(28, 32, 0, 32));
        headerRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        if (session.isAdmin()) {
            Button addBtn = createGoldButton("+ Ajouter");
            addBtn.setOnAction(e -> showForm(null, new FlowPane()));
            headerRow.getChildren().addAll(titleBox, addBtn);
        } else {
            headerRow.getChildren().add(titleBox);
        }

        FlowPane cards = new FlowPane();
        cards.setHgap(18);
        cards.setVgap(18);
        cards.setPadding(new Insets(24, 32, 32, 32));

        refreshCards(cards);

        root.getChildren().addAll(headerRow, cards);
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1a1a1a; -fx-background-color: #1a1a1a; -fx-border-color: transparent;");
        return scroll;
    }

    // ── Refresh ───────────────────────────────────────────────────
    private void refreshCards(FlowPane cards) {
        cards.getChildren().clear();

        // Charger la map des produits
        productMap = productService.getAll().stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

        List<Wishlist> list;
        if (session.isClient()) {
            // Client voit seulement SES favoris
            list = service.getAll().stream()
                    .filter(w -> session.getName().equals(w.getClientName()))
                    .collect(Collectors.toList());
        } else {
            list = service.getAll();
        }

        if (list.isEmpty()) {
            Label empty = new Label(session.isClient()
                    ? "Vous n'avez pas encore de produits en favoris."
                    : "Aucun favori trouvé.");
            empty.setStyle("-fx-text-fill: #555; -fx-font-size: 13;");
            empty.setPadding(new Insets(32));
            cards.getChildren().add(empty);
            return;
        }
        for (Wishlist w : list)
            cards.getChildren().add(buildCard(w, cards));
    }

    // ── Card ──────────────────────────────────────────────────────
    private Node buildCard(Wishlist w, FlowPane cards) {
        String normalStyle = "-fx-background-color: #222; -fx-background-radius: 12; " +
                "-fx-border-color: #333; -fx-border-radius: 12; -fx-border-width: 1;";
        String hoverStyle  = "-fx-background-color: #252525; -fx-background-radius: 12; " +
                "-fx-border-color: #c0c0c0; -fx-border-radius: 12; -fx-border-width: 1.5;";

        VBox card = new VBox(10);
        card.setPrefWidth(270);
        card.setPadding(new Insets(18));
        card.setStyle(normalStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e  -> card.setStyle(normalStyle));

        // ── Heart + client name ───────────────────────────────────
        Label heart  = new Label("♥");
        heart.setStyle("-fx-text-fill: #e57373; -fx-font-size: 18;");
        Label client = new Label(w.getClientName());
        client.setStyle("-fx-text-fill: #fff; -fx-font-weight: bold; -fx-font-size: 15;");
        HBox header = new HBox(8, heart, client);
        header.setAlignment(Pos.CENTER_LEFT);

        // ── Product info ──────────────────────────────────────────
        Product prod = productMap.get(w.getProductId());
        String prodName = prod != null ? prod.getName() : "#" + w.getProductId();

        Label prodLbl = new Label("📦  " + prodName);
        prodLbl.setStyle("-fx-background-color: #2d2600; -fx-text-fill: #c0c0c0; -fx-font-size: 12; " +
                "-fx-background-radius: 5; -fx-padding: 3 8 3 8;");
        prodLbl.setWrapText(true);

        // Price if product exists
        Label priceLbl = new Label("");
        if (prod != null && prod.getPrice() != null) {
            priceLbl.setText(prod.getPrice().toPlainString() + " €");
            priceLbl.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 16; -fx-font-weight: bold;");
        }

        // Status badge
        Label statusLbl = new Label("");
        if (prod != null) {
            boolean available = "available".equals(prod.getStatus());
            statusLbl.setText(available ? "✔ Disponible" : "✖ Vendu");
            statusLbl.setStyle("-fx-text-fill: " + (available ? "#7ec97e" : "#e57373") +
                    "; -fx-font-size: 11;");
        }

        String dateStr = w.getCreatedAt() != null
                ? w.getCreatedAt().toString().replace("T", " ").substring(0, 16) : "-";
        Label date = new Label("📅  Ajouté le " + dateStr);
        date.setStyle("-fx-text-fill: #555; -fx-font-size: 11;");

        // ── Buttons depending on role ─────────────────────────────
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #333;");

        if (session.isAdmin()) {
            // Admin: éditer + supprimer
            HBox buttons = new HBox(8);
            Button editBtn = createOutlineButton("✎ Éditer");
            editBtn.setOnAction(e -> showForm(w, cards));
            Button deleteBtn = createDeleteButton("✕ Supprimer");
            deleteBtn.setOnAction(e -> {
                Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                        "Supprimer de la liste de souhaits ?",
                        ButtonType.YES, ButtonType.NO);
                a.setHeaderText(null);
                a.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        service.delete(w.getId());
                        refreshCards(cards);
                    }
                });
            });
            buttons.getChildren().addAll(editBtn, deleteBtn);
            card.getChildren().addAll(header, prodLbl, priceLbl, statusLbl, date, sep, buttons);

        } else if (session.isClient()) {
            // Client: bouton Acheter (si produit available) + Retirer de la liste
            HBox btnRow = new HBox(8);
            btnRow.setAlignment(Pos.CENTER_LEFT);

            boolean available = prod != null && "available".equals(prod.getStatus());
            Button buyBtn = new Button(available ? "🛒  Acheter" : "✖ Vendu");
            buyBtn.setStyle(available
                    ? "-fx-background-color: #c0c0c0; -fx-text-fill: #111; -fx-font-weight: bold; " +
                      "-fx-background-radius: 7; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 13;"
                    : "-fx-background-color: #333; -fx-text-fill: #666; " +
                      "-fx-background-radius: 7; -fx-padding: 8 16; -fx-cursor: default;");
            buyBtn.setDisable(!available);

            if (available) {
                buyBtn.setOnMouseEntered(e -> buyBtn.setStyle(
                        "-fx-background-color: #dcdcdc; -fx-text-fill: #111; -fx-font-weight: bold; " +
                        "-fx-background-radius: 7; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 13;"));
                buyBtn.setOnMouseExited(e -> buyBtn.setStyle(
                        "-fx-background-color: #c0c0c0; -fx-text-fill: #111; -fx-font-weight: bold; " +
                        "-fx-background-radius: 7; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 13;"));
                buyBtn.setOnAction(e -> showBuyFromWishlist(prod, w.getId(), cards));
            }

            Button removeBtn = createDeleteButton("✕");
            removeBtn.setTooltip(new Tooltip("Retirer de la liste"));
            removeBtn.setOnAction(e -> {
                service.delete(w.getId());
                refreshCards(cards);
            });

            HBox.setHgrow(buyBtn, Priority.ALWAYS);
            buyBtn.setMaxWidth(Double.MAX_VALUE);
            btnRow.getChildren().addAll(buyBtn, removeBtn);
            card.getChildren().addAll(header, prodLbl, priceLbl, statusLbl, date, sep, btnRow);

        } else {
            // Artist: lecture seule
            Label hint = new Label("ℹ  Consultation uniquement");
            hint.setStyle("-fx-text-fill: #444; -fx-font-size: 10; -fx-font-style: italic;");
            card.getChildren().addAll(header, prodLbl, priceLbl, statusLbl, date, hint);
        }
        return card;
    }

    // ── Buy directly from wishlist ────────────────────────────────
    private void showBuyFromWishlist(Product p, int wishlistId, FlowPane cards) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Confirmer l'achat");
        dialog.setWidth(420);
        dialog.setResizable(false);

        VBox layout = new VBox(16);
        layout.setPadding(new Insets(28, 32, 24, 32));
        layout.setStyle("-fx-background-color: #1a1a1a;");

        Label title = new Label("Confirmer l'achat");
        title.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 18; -fx-font-weight: bold;");

        // Product summary
        VBox summaryBox = new VBox(6);
        summaryBox.setPadding(new Insets(14));
        summaryBox.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10; " +
                "-fx-border-color: #333; -fx-border-radius: 10; -fx-border-width: 1;");
        Label pName = new Label(p.getName());
        pName.setStyle("-fx-text-fill: #fff; -fx-font-size: 14; -fx-font-weight: bold;");
        pName.setWrapText(true);
        Label pPrice = new Label("Prix : " + (p.getPrice() != null ? p.getPrice().toPlainString() + " €" : "—"));
        pPrice.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 15; -fx-font-weight: bold;");
        summaryBox.getChildren().addAll(pName, pPrice);

        // Buyer name pre-filled
        Label buyerLbl = new Label("Votre nom *");
        buyerLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
        TextField tfBuyer = styledField(session.getName());

        Label errLbl = new Label("");
        errLbl.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11;");

        Button confirmBtn = dialogGoldBtn("✓  Confirmer");
        Button cancelBtn  = dialogOutlineBtn("✕  Annuler");
        cancelBtn.setOnAction(e -> dialog.close());

        confirmBtn.setOnAction(e -> {
            String buyer = tfBuyer.getText().trim();
            if (buyer.isBlank() || buyer.length() < 2) {
                errLbl.setText("Nom invalide (min 2 caractères).");
                return;
            }

            Order order = new Order();
            order.setBuyerName(buyer);
            order.setPrice(p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO);
            order.setOrderType(p.getSaleType() != null ? p.getSaleType() : "fixed");
            order.setProductId(p.getId());
            orderService.add(order);
            productService.markAsSold(p.getId());

            // Retirer de la wishlist
            service.delete(wishlistId);

            dialog.close();
            showSuccessToast(p.getName(), buyer);
            refreshCards(cards);
        });

        HBox btnRow = new HBox(10, confirmBtn, cancelBtn);
        HBox.setHgrow(confirmBtn, Priority.ALWAYS);
        HBox.setHgrow(cancelBtn,  Priority.ALWAYS);

        layout.getChildren().addAll(title, summaryBox, buyerLbl, tfBuyer, errLbl, btnRow);
        dialog.setScene(new Scene(layout));
        dialog.show();
    }

    // ── Success toast ─────────────────────────────────────────────
    private void showSuccessToast(String productName, String buyer) {
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.setTitle("Achat confirmé !");
        s.setWidth(340);
        s.setResizable(false);
        VBox box = new VBox(14);
        box.setPadding(new Insets(28, 28, 20, 28));
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #1a1a1a;");
        Label icon = new Label("✅");
        icon.setStyle("-fx-font-size: 38;");
        Label msg = new Label("Merci " + buyer + " !\n« " + productName + " » acheté avec succès.");
        msg.setStyle("-fx-text-fill: #a8d5a2; -fx-font-size: 13; -fx-font-weight: bold;");
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        msg.setWrapText(true);
        Button okBtn = dialogGoldBtn("Fermer");
        okBtn.setOnAction(e -> s.close());
        box.getChildren().addAll(icon, msg, okBtn);
        s.setScene(new Scene(box));
        s.show();
    }

    // ── Admin form (add/edit) ─────────────────────────────────────
    private void showForm(Wishlist existing, FlowPane cards) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle(existing == null ? "Ajouter un favori" : "Modifier le favori");
        popup.setWidth(420);
        popup.setResizable(false);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(28));
        grid.setStyle("-fx-background-color: #1a1a1a;");

        TextField tfClient    = styledField(existing != null ? existing.getClientName() : "");
        TextField tfProductId = styledField(existing != null ? String.valueOf(existing.getProductId()) : "");

        grid.add(fl("Client *"), 0, 0);
        grid.add(tfClient, 1, 0);
        grid.add(fl("Produit ID *"), 0, 1);
        grid.add(tfProductId, 1, 1);

        Button saveBtn   = createGoldButton(existing == null ? "Enregistrer" : "Mettre à jour");
        Button cancelBtn = createOutlineButton("Annuler");
        cancelBtn.setOnAction(e -> popup.close());

        saveBtn.setOnAction(e -> {
            if (tfClient.getText().isBlank() || tfProductId.getText().isBlank()) return;
            try {
                Wishlist w = existing != null ? existing : new Wishlist();
                w.setClientName(tfClient.getText().trim());
                w.setProductId(Integer.parseInt(tfProductId.getText().trim()));
                if (existing == null) service.add(w);
                else service.update(w);
                popup.close();
                refreshCards(cards);
            } catch (NumberFormatException ex) { /* ignore */ }
        });

        HBox actions = new HBox(10, saveBtn, cancelBtn);
        actions.setPadding(new Insets(10, 28, 20, 28));
        VBox layout = new VBox(grid, actions);
        layout.setStyle("-fx-background-color: #1a1a1a;");
        popup.setScene(new Scene(layout));
        popup.show();
    }

    // ── Style helpers ─────────────────────────────────────────────
    private Button createGoldButton(String t) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color: #c0c0c0; -fx-text-fill: #111; -fx-font-weight: bold; " +
                "-fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #dcdcdc; -fx-text-fill: #111; -fx-font-weight: bold; " +
                "-fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;"));
        b.setOnMouseExited(e  -> b.setStyle("-fx-background-color: #c0c0c0; -fx-text-fill: #111; -fx-font-weight: bold; " +
                "-fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;"));
        return b;
    }

    private Button createOutlineButton(String t) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccc; " +
                "-fx-border-color: #555; -fx-border-radius: 6; -fx-background-radius: 6; " +
                "-fx-padding: 6 14; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #333; -fx-text-fill: #fff; " +
                "-fx-border-color: #888; -fx-border-radius: 6; -fx-background-radius: 6; " +
                "-fx-padding: 6 14; -fx-cursor: hand;"));
        b.setOnMouseExited(e  -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccc; " +
                "-fx-border-color: #555; -fx-border-radius: 6; -fx-background-radius: 6; " +
                "-fx-padding: 6 14; -fx-cursor: hand;"));
        return b;
    }

    private Button createDeleteButton(String t) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #999; " +
                "-fx-border-color: #444; -fx-border-radius: 6; -fx-background-radius: 6; " +
                "-fx-padding: 6 12; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #4a1a1a; -fx-text-fill: #ff6b6b; " +
                "-fx-border-color: #8b3a3a; -fx-border-radius: 6; -fx-background-radius: 6; " +
                "-fx-padding: 6 12; -fx-cursor: hand;"));
        b.setOnMouseExited(e  -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: #999; " +
                "-fx-border-color: #444; -fx-border-radius: 6; -fx-background-radius: 6; " +
                "-fx-padding: 6 12; -fx-cursor: hand;"));
        return b;
    }

    private Button dialogGoldBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #c0c0c0; -fx-text-fill: #111; -fx-font-weight: bold; " +
                "-fx-background-radius: 7; -fx-padding: 10 20; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #e0be6a; -fx-text-fill: #111; -fx-font-weight: bold; " +
                "-fx-background-radius: 7; -fx-padding: 10 20; -fx-cursor: hand;"));
        b.setOnMouseExited(e  -> b.setStyle("-fx-background-color: #c0c0c0; -fx-text-fill: #111; -fx-font-weight: bold; " +
                "-fx-background-radius: 7; -fx-padding: 10 20; -fx-cursor: hand;"));
        return b;
    }

    private Button dialogOutlineBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccc; " +
                "-fx-border-color: #555; -fx-border-radius: 7; -fx-background-radius: 7; " +
                "-fx-padding: 10 20; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #fff; " +
                "-fx-border-color: #888; -fx-border-radius: 7; -fx-background-radius: 7; " +
                "-fx-padding: 10 20; -fx-cursor: hand;"));
        b.setOnMouseExited(e  -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccc; " +
                "-fx-border-color: #555; -fx-border-radius: 7; -fx-background-radius: 7; " +
                "-fx-padding: 10 20; -fx-cursor: hand;"));
        return b;
    }

    private TextField styledField(String val) {
        TextField tf = new TextField(val);
        tf.setStyle("-fx-control-inner-background: #2a2a2a; " +
                "-fx-background-color: -fx-control-inner-background; " +
                "-fx-text-fill: #ffffff; " +
                "-fx-prompt-text-fill: derive(-fx-control-inner-background, +80%); " +
                "-fx-border-color: #444; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");
        tf.setPrefWidth(240);
        return tf;
    }

    private Label fl(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12;");
        l.setMinWidth(100);
        return l;
    }
}
