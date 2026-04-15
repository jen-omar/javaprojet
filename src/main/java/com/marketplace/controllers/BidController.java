package com.marketplace.controllers;

import com.marketplace.models.Bid;
import com.marketplace.models.Product;
import com.marketplace.services.BidService;
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
 * BidController — handles the Bids (Enchères) view.
 * Uses BidService for all CRUD operations.
 */
public class BidController {

    private final BidService service = new BidService();
    private final ProductService productService = new ProductService();
    private final SessionManager session = SessionManager.getInstance();
    private Map<Integer, String> pNames = new java.util.HashMap<>();

    public Node buildView() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1a1a1a;");

        Label title = new Label("ENCHÈRES");
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 22; -fx-font-weight: bold;");
        Label subtitle = new Label("Liste des offres sur les produits aux enchères");
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

        addBtn.setOnAction(e -> showForm(null, cards));
        if (session.isArtist())
            addBtn.setVisible(false);
        refreshCards(cards);

        root.getChildren().addAll(headerRow, cards);
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1a1a1a; -fx-background-color: #1a1a1a; -fx-border-color: transparent;");
        return scroll;
    }

    private void refreshCards(FlowPane cards) {
        cards.getChildren().clear();
        List<Bid> bids = service.getAll();
        if (session.isArtist()) {
            Set<Integer> myProductIds = productService.getAll().stream()
                    .filter(p -> session.getName().equalsIgnoreCase(
                            p.getArtistName() != null ? p.getArtistName() : ""))
                    .map(Product::getId)
                    .collect(Collectors.toSet());
            bids = bids.stream()
                    .filter(b -> myProductIds.contains(b.getProductId()))
                    .collect(Collectors.toList());
        }
        if (bids.isEmpty()) {
            Label empty = new Label("Aucune enchère trouvée.");
            empty.setStyle("-fx-text-fill: #666; -fx-font-size: 14;");
            cards.getChildren().add(empty);
        }
        pNames = productService.getAll().stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));
        for (Bid b : bids)
            cards.getChildren().add(buildCard(b, cards));
    }

    private Node buildCard(Bid b, FlowPane cards) {
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

        // Header row: icon + bidder name
        Label icon = new Label("⚡");
        icon.setStyle("-fx-text-fill: #ff9800; -fx-font-size: 16;");
        Label bidder = new Label(b.getBidderName());
        bidder.setStyle("-fx-text-fill: #fff; -fx-font-weight: bold; -fx-font-size: 15;");
        HBox header = new HBox(8, icon, bidder);
        header.setAlignment(Pos.CENTER_LEFT);

        // Amount — gold if it’s a high bid
        Label amount = new Label(b.getAmount() + " €");
        amount.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 22; -fx-font-weight: bold;");
        Label amountLbl = new Label("Offre proposée");
        amountLbl.setStyle("-fx-text-fill: #666; -fx-font-size: 10;");

        Label prod = new Label("📦  #" + b.getProductId()
                + (pNames.containsKey(b.getProductId()) ? " — " + pNames.get(b.getProductId()) : ""));
        prod.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");

        String dateStr = b.getCreatedAt() != null
                ? b.getCreatedAt().toString().replace("T", " ").substring(0, 16)
                : "-";
        Label date = new Label("📅  " + dateStr);
        date.setStyle("-fx-text-fill: #555; -fx-font-size: 11;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #333;");

        HBox buttons = new HBox(8);
        if (session.isAdmin()) {
            Button editBtn = createOutlineButton("✎ Éditer");
            editBtn.setOnAction(e -> showForm(b, cards));
            buttons.getChildren().add(editBtn);
        }
        Button deleteBtn = createDeleteButton("✕ Supprimer");
        deleteBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cette enchère ?",
                    ButtonType.YES, ButtonType.NO);
            alert.setHeaderText(null);
            alert.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    service.delete(b.getId());
                    refreshCards(cards);
                }
            });
        });
        buttons.getChildren().add(deleteBtn);
        card.getChildren().addAll(header, amount, amountLbl, prod, date, sep, buttons);
        return card;
    }

    private void showForm(Bid existing, FlowPane cards) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle(existing == null ? "Ajouter une enchère" : "Modifier l'enchère");
        popup.setWidth(420);
        popup.setHeight(300);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(28));
        grid.setStyle("-fx-background-color: #1a1a1a;");

        TextField tfBidder = styledField(existing != null ? existing.getBidderName() : "");
        TextField tfAmount = styledField(existing != null ? existing.getAmount().toPlainString() : "");
        TextField tfProductId = styledField(existing != null ? String.valueOf(existing.getProductId()) : "");

        int row = 0;
        grid.add(formLabel("Enchérisseur *"), 0, row);
        grid.add(tfBidder, 1, row++);
        grid.add(formLabel("Montant (€) *"), 0, row);
        grid.add(tfAmount, 1, row++);
        grid.add(formLabel("Produit ID *"), 0, row);
        grid.add(tfProductId, 1, row++);

        Button saveBtn = createGoldButton(existing == null ? "Enregistrer" : "Mettre à jour");
        Button cancelBtn = createOutlineButton("Annuler");
        cancelBtn.setOnAction(e -> popup.close());

        saveBtn.setOnAction(e -> {
            if (tfBidder.getText().isBlank() || tfAmount.getText().isBlank() || tfProductId.getText().isBlank())
                return;
            try {
                Bid bid = existing != null ? existing : new Bid();
                bid.setBidderName(tfBidder.getText().trim());
                bid.setAmount(new BigDecimal(tfAmount.getText().trim()));
                bid.setProductId(Integer.parseInt(tfProductId.getText().trim()));
                if (existing == null)
                    service.add(bid);
                else
                    service.update(bid);
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
                "-fx-text-fill: #ffffff; -fx-prompt-text-fill: #555; " +
                "-fx-border-color: #444; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");
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
        l.setMinWidth(120);
        return l;
    }
}
