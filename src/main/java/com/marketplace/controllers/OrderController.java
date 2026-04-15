package com.marketplace.controllers;

import com.marketplace.models.Order;
import com.marketplace.models.Product;
import com.marketplace.services.OrderService;
import com.marketplace.services.ProductService;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OrderController — handles the Orders (Commandes) view.
 * Uses OrderService for all CRUD operations.
 */
public class OrderController {

    private final OrderService service = new OrderService();
    private final ProductService productService = new ProductService();
    private final SessionManager session = SessionManager.getInstance();
    private Map<Integer, String> pNames = new java.util.HashMap<>();

    // ── Main View ─────────────────────────────────────────────────
    public Node buildView() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1a1a1a;");

        Label title = new Label(session.isArtist() ? "MES COMMANDES" : "COMMANDES");
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 22; -fx-font-weight: bold;");
        Label subtitle = new Label(session.isArtist()
                ? "Commandes passées sur vos produits"
                : "Gérer toutes les commandes du marketplace");
        subtitle.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");
        VBox titleBox = new VBox(2, title, subtitle);

        Button addBtn = createGoldButton("+ Ajouter");

        HBox headerRow = new HBox();
        headerRow.setPadding(new Insets(28, 32, 0, 32));
        headerRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        headerRow.getChildren().addAll(titleBox, addBtn);

        FlowPane cards = new FlowPane();
        cards.setHgap(16);
        cards.setVgap(16);
        cards.setPadding(new Insets(24, 32, 32, 32));

        // Hide add button for artist (they only consult with status change)
        if (session.isArtist())
            addBtn.setVisible(false);
        addBtn.setOnAction(e -> showForm(null, cards));
        refreshCards(cards);

        root.getChildren().addAll(headerRow, cards);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1a1a1a; -fx-background-color: #1a1a1a; -fx-border-color: transparent;");
        return scroll;
    }

    private void refreshCards(FlowPane cards) {
        cards.getChildren().clear();
        List<Order> orders = service.getAll();
        if (session.isArtist()) {
            Set<Integer> myProductIds = productService.getAll().stream()
                    .filter(p -> session.getName().equalsIgnoreCase(
                            p.getArtistName() != null ? p.getArtistName() : ""))
                    .map(Product::getId)
                    .collect(Collectors.toSet());
            orders = orders.stream()
                    .filter(o -> myProductIds.contains(o.getProductId()))
                    .collect(Collectors.toList());
        }
        if (orders.isEmpty()) {
            Label empty = new Label("Aucune commande trouvée.");
            empty.setStyle("-fx-text-fill: #666; -fx-font-size: 14;");
            cards.getChildren().add(empty);
        }
        pNames = productService.getAll().stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));
        for (Order o : orders)
            cards.getChildren().add(buildCard(o, cards));
    }

    private Node buildCard(Order o, FlowPane cards) {
        String normalStyle = "-fx-background-color: #222; -fx-background-radius: 12; " +
                "-fx-border-color: #333; -fx-border-radius: 12; -fx-border-width: 1;";
        String hoverStyle = "-fx-background-color: #2a2a2a; -fx-background-radius: 12; " +
                "-fx-border-color: #c9a84c; -fx-border-radius: 12; -fx-border-width: 1.5;";
        VBox card = new VBox(10);
        card.setPrefWidth(290);
        card.setPadding(new Insets(18));
        card.setStyle(normalStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(normalStyle));

        // ── Type badge (green=completed, orange=auction, teal=purchase, grey=other)
        String type = o.getOrderType() != null ? o.getOrderType() : "";
        String badgeBg = switch (type) {
            case "completed" -> "#1c4a2e";
            case "auction" -> "#4a3200";
            case "purchase" -> "#1a3a4a";
            default -> "#333";
        };
        String badgeFg = switch (type) {
            case "completed" -> "#4caf50";
            case "auction" -> "#ff9800";
            case "purchase" -> "#4fc3f7";
            default -> "#888";
        };
        String badgeTxt = switch (type) {
            case "completed" -> "✓ Terminé";
            case "auction" -> "⚡ Enchère";
            case "purchase" -> "◎ Achat";
            default -> type.isEmpty() ? "—" : type;
        };
        Label badge = new Label(badgeTxt);
        badge.setStyle("-fx-background-color: " + badgeBg + "; -fx-text-fill: " + badgeFg +
                "; -fx-font-size: 11; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 3 8 3 8;");

        if (session.isArtist()) {
            // ── ARTIST view ──
            Label prodLabel = new Label("📦  #" + o.getProductId()
                    + (pNames.containsKey(o.getProductId()) ? " — " + pNames.get(o.getProductId()) : ""));
            prodLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 14;");
            Label priceLabel = new Label(o.getPrice() + " €");
            priceLabel.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 20; -fx-font-weight: bold;");
            String ds = o.getCreatedAt() != null ? o.getCreatedAt().toString().substring(0, 10) : "-";
            Label dateLabel = new Label("📅  " + ds);
            dateLabel.setStyle("-fx-text-fill: #777; -fx-font-size: 12;");

            // disable done button if already completed
            Button doneBtn = createGoldButton("✓ Marquer terminé");
            doneBtn.setDisable("completed".equals(type));
            doneBtn.setOnAction(e -> {
                o.setOrderType("completed");
                service.update(o);
                refreshCards(cards);
            });
            Button delBtn = createDeleteButton("✕ Supprimer");
            delBtn.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Supprimer cette commande ?", ButtonType.YES, ButtonType.NO);
                alert.setHeaderText(null);
                alert.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        service.delete(o.getId());
                        refreshCards(cards);
                    }
                });
            });
            card.getChildren().addAll(badge, prodLabel, priceLabel, dateLabel,
                    new HBox(8, doneBtn, delBtn));
        } else {
            // ── ADMIN view ──
            Label buyer = new Label("👤  " + o.getBuyerName());
            buyer.setStyle("-fx-text-fill: #fff; -fx-font-weight: bold; -fx-font-size: 15;");
            Label price = new Label(o.getPrice() + " €");
            price.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 20; -fx-font-weight: bold;");
            Label prod = new Label("📦  #" + o.getProductId()
                    + (pNames.containsKey(o.getProductId()) ? " — " + pNames.get(o.getProductId()) : ""));
            prod.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
            String ds = o.getCreatedAt() != null
                    ? o.getCreatedAt().toString().replace("T", " ").substring(0, 16)
                    : "-";
            Label date = new Label("📅  " + ds);
            date.setStyle("-fx-text-fill: #555; -fx-font-size: 11;");
            HBox buttons = new HBox(8);
            Button editBtn = createOutlineButton("✎ Éditer");
            Button deleteBtn = createDeleteButton("✕ Supprimer");
            editBtn.setOnAction(e -> showForm(o, cards));
            deleteBtn.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Supprimer cette commande ?", ButtonType.YES, ButtonType.NO);
                alert.setHeaderText(null);
                alert.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        service.delete(o.getId());
                        refreshCards(cards);
                    }
                });
            });
            buttons.getChildren().addAll(editBtn, deleteBtn);
            Separator sep = new Separator();
            sep.setStyle("-fx-background-color: #333;");
            card.getChildren().addAll(badge, buyer, price, prod, date, sep, buttons);
        }
        return card;
    }

    private void showForm(Order existing, FlowPane cards) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle(existing == null ? "Ajouter une commande" : "Modifier la commande");
        popup.setWidth(420);
        popup.setHeight(350);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(28));
        grid.setStyle("-fx-background-color: #1a1a1a;");

        TextField tfBuyer = styledField(existing != null ? existing.getBuyerName() : "");
        TextField tfPrice = styledField(existing != null ? existing.getPrice().toPlainString() : "");

        int row = 0;
        grid.add(formLabel("Acheteur *"), 0, row);
        grid.add(tfBuyer, 1, row++);
        grid.add(formLabel("Prix (€) *"), 0, row);
        grid.add(tfPrice, 1, row++);

        // When editing: lock product ID and order type (read-only)
        if (existing != null) {
            Label lbType = new Label(existing.getOrderType());
            lbType.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
            Label lbProd = new Label("Produit #" + existing.getProductId());
            lbProd.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");
            grid.add(formLabel("Type"), 0, row);
            grid.add(lbType, 1, row++);
            grid.add(formLabel("Produit"), 0, row);
            grid.add(lbProd, 1, row++);
        } else {
            // New order: allow setting product and type
            TextField tfProductId = styledField("");
            ComboBox<String> cbType = styledCombo("purchase", "auction");
            grid.add(formLabel("Type"), 0, row);
            grid.add(cbType, 1, row++);
            grid.add(formLabel("Produit ID *"), 0, row);
            grid.add(tfProductId, 1, row++);
            // Wire save with product/type for new orders
            Button saveBtn2 = createGoldButton("Enregistrer");
            Button cancelBtn2 = createOutlineButton("Annuler");
            cancelBtn2.setOnAction(e -> popup.close());
            saveBtn2.setOnAction(e -> {
                if (tfBuyer.getText().isBlank() || tfPrice.getText().isBlank() || tfProductId.getText().isBlank())
                    return;
                try {
                    Order o = new Order();
                    o.setBuyerName(tfBuyer.getText().trim());
                    o.setPrice(new BigDecimal(tfPrice.getText().trim()));
                    o.setOrderType(cbType.getValue());
                    o.setProductId(Integer.parseInt(tfProductId.getText().trim()));
                    service.add(o);
                    popup.close();
                    refreshCards(cards);
                } catch (NumberFormatException ex) {
                    /* ignore */ }
            });
            HBox actions2 = new HBox(10, saveBtn2, cancelBtn2);
            actions2.setPadding(new Insets(10, 28, 20, 28));
            VBox layout2 = new VBox(grid, actions2);
            layout2.setStyle("-fx-background-color: #1a1a1a;");
            popup.setScene(new Scene(layout2));
            popup.show();
            return; // early return — new-order path handled above
        }

        Button saveBtn = createGoldButton(existing == null ? "Enregistrer" : "Mettre à jour");
        Button cancelBtn = createOutlineButton("Annuler");
        cancelBtn.setOnAction(e -> popup.close());

        saveBtn.setOnAction(e -> {
            if (tfBuyer.getText().isBlank() || tfPrice.getText().isBlank())
                return;
            try {
                existing.setBuyerName(tfBuyer.getText().trim());
                existing.setPrice(new BigDecimal(tfPrice.getText().trim()));
                // productId and orderType are locked — keep existing values
                service.update(existing);
                popup.close();
                refreshCards(cards);
            } catch (NumberFormatException ex) {
                /* ignore */ }
        });

        HBox actions = new HBox(10, saveBtn, cancelBtn);
        actions.setPadding(new Insets(10, 28, 20, 28));
        VBox layout = new VBox(grid, actions);
        layout.setStyle("-fx-background-color: #1a1a1a;");

        popup.setScene(new Scene(layout));
        popup.show();
    }

    // ── Shared UI Helpers ─────────────────────────────────────────
    private Button createGoldButton(String t) {
        Button b = new Button(t);
        b.setStyle(
                "-fx-background-color: #c9a84c; -fx-text-fill: #1a1a1a; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        return b;
    }

    private Button createOutlineButton(String t) {
        Button b = new Button(t);
        b.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #ccc; -fx-border-color: #555; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle(
                "-fx-background-color: #333; -fx-text-fill: #fff; -fx-border-color: #888; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 14 6 14; -fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #ccc; -fx-border-color: #555; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 14 6 14; -fx-cursor: hand;"));
        return b;
    }

    private Button createDeleteButton(String t) {
        Button b = new Button(t);
        b.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #999; -fx-border-color: #444; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle(
                "-fx-background-color: #4a1a1a; -fx-text-fill: #ff6b6b; -fx-border-color: #8b3a3a; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 14 6 14; -fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #999; -fx-border-color: #444; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 14 6 14; -fx-cursor: hand;"));
        return b;
    }

    private TextField styledField(String val) {
        TextField tf = new TextField(val);
        tf.setStyle("-fx-control-inner-background: #2a2a2a; -fx-background-color: #2a2a2a; " +
                "-fx-text-fill: #ffffff; -fx-prompt-text-fill: #666; " +
                "-fx-border-color: #444; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 7;");
        tf.setPrefWidth(260);
        return tf;
    }

    private ComboBox<String> styledCombo(String... items) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll(items);
        cb.setPrefWidth(260);
        if (items.length > 0)
            cb.setValue(items[0]);
        cb.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #444; -fx-border-radius: 5;");
        cb.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle("-fx-text-fill: #ffffff; -fx-background-color: #2a2a2a;");
            }
        });
        return cb;
    }

    private Label formLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12;");
        l.setMinWidth(110);
        return l;
    }
}
