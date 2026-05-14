package com.marketplace.controllers;

import com.marketplace.models.Review;
import com.marketplace.models.Product;
import com.marketplace.services.ProductService;
import com.marketplace.services.ReviewService;
import com.marketplace.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** ReviewController — handles the Avis view. */
public class ReviewController {

    private final ReviewService service = new ReviewService();
    private final ProductService productService = new ProductService();
    private final SessionManager session = SessionManager.getInstance();
    private Map<Integer, String> pNames = new java.util.HashMap<>();

    public Node buildView() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1a1a1a;");

        Label title = new Label("AVIS");
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 22; -fx-font-weight: bold;");
        String subText = session.isArtist() 
                ? "Retours de vos clients sur vos créations" 
                : "Évaluations des clients sur les produits";
        Label subtitle = new Label(subText);
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
        if (session.isArtist() || session.isClient())
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
        List<Review> list = service.getAll();
        if (session.isArtist()) {
            // Only show reviews for the artist's own products
            Set<Integer> myProductIds = productService.getAll().stream()
                    .filter(p -> session.getName().equalsIgnoreCase(p.getArtistName() != null ? p.getArtistName() : ""))
                    .map(Product::getId)
                    .collect(Collectors.toSet());
            list = list.stream()
                    .filter(r -> myProductIds.contains(r.getProductId()))
                    .collect(Collectors.toList());
        }
        if (list.isEmpty()) {
            Label e = new Label("Aucun avis trouvé.");
            e.setStyle("-fx-text-fill: #666;");
            cards.getChildren().add(e);
            return;
        }
        pNames = productService.getAll().stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));
        for (Review r : list)
            cards.getChildren().add(buildCard(r, cards));
    }

    private Node buildCard(Review r, FlowPane cards) {
        String normalStyle = "-fx-background-color: #222; -fx-background-radius: 12; " +
                "-fx-border-color: #333; -fx-border-radius: 12; -fx-border-width: 1;";
        String hoverStyle = "-fx-background-color: #2a2a2a; -fx-background-radius: 12; " +
                "-fx-border-color: #c0c0c0; -fx-border-radius: 12; -fx-border-width: 1.5;";
        VBox card = new VBox(10);
        card.setPrefWidth(290);
        card.setPadding(new Insets(18));
        card.setStyle(normalStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(normalStyle));

        // Color-coded rating: 5=green, 4=lime, 3=orange, 1-2=red
        int rating = r.getRating();
        String starColor = rating >= 5 ? "#4caf50"
                : rating == 4 ? "#8bc34a"
                        : rating == 3 ? "#ff9800" : "#f44336";
        Label stars = new Label(r.getStars() + "  " + rating + "/5");
        stars.setStyle("-fx-text-fill: " + starColor + "; -fx-font-size: 15; -fx-font-weight: bold;");

        Label reviewer = new Label("\uD83D\uDC64  " + r.getReviewerName());
        reviewer.setStyle("-fx-text-fill: #fff; -fx-font-weight: bold; -fx-font-size: 14;");

        String ct = r.getComment() != null && !r.getComment().isBlank() ? r.getComment() : "Aucun commentaire.";
        Label comment = new Label(ct.length() > 80 ? ct.substring(0, 80) + "\u2026" : ct);
        comment.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12; -fx-font-style: italic;");
        comment.setWrapText(true);

        Label prod = new Label("📦  #" + r.getProductId()
                + (pNames.containsKey(r.getProductId()) ? " — " + pNames.get(r.getProductId()) : ""));
        prod.setStyle("-fx-text-fill: #555; -fx-font-size: 11;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #333;");

        HBox buttons = new HBox(8);
        if (session.isAdmin()) {
            Button editBtn = createOutlineButton("✎ Éditer");
            editBtn.setOnAction(e -> showForm(r, cards));
            buttons.getChildren().add(editBtn);

            Button deleteBtn = createDeleteButton("✕ Supprimer");
            deleteBtn.setOnAction(e -> {
                Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cet avis ?", ButtonType.YES, ButtonType.NO);
                a.setHeaderText(null);
                a.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        service.delete(r.getId());
                        refreshCards(cards);
                    }
                });
            });
            buttons.getChildren().add(deleteBtn);
        }

        if (buttons.getChildren().isEmpty()) {
            card.getChildren().addAll(stars, reviewer, comment, prod);
        } else {
            card.getChildren().addAll(stars, reviewer, comment, prod, sep, buttons);
        }
        return card;
    }

    private void showForm(Review existing, FlowPane cards) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle(existing == null ? "Ajouter un avis" : "Modifier l'avis");
        popup.setWidth(420);
        popup.setHeight(340);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(28));
        grid.setStyle("-fx-background-color: #1a1a1a;");

        TextField tfReviewer = styledField(existing != null ? existing.getReviewerName() : "");
        ComboBox<Integer> cbRating = new ComboBox<>();
        cbRating.getItems().addAll(1, 2, 3, 4, 5);
        cbRating.setValue(existing != null ? existing.getRating() : 5);
        cbRating.setPrefWidth(260);
        cbRating.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #444; -fx-border-radius: 5;");
        cbRating.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item + " ★");
                setStyle("-fx-text-fill: #ffffff; -fx-background-color: #2a2a2a;");
            }
        });
        TextArea taComment = styledArea(existing != null && existing.getComment() != null ? existing.getComment() : "");
        TextField tfProductId = styledField(existing != null ? String.valueOf(existing.getProductId()) : "");

        int row = 0;
        grid.add(fl("Auteur *"), 0, row);
        grid.add(tfReviewer, 1, row++);
        grid.add(fl("Note (1–5)"), 0, row);
        grid.add(cbRating, 1, row++);
        grid.add(fl("Commentaire"), 0, row);
        grid.add(taComment, 1, row++);
        grid.add(fl("Produit ID *"), 0, row);
        grid.add(tfProductId, 1, row++);

        Button saveBtn = createGoldButton(existing == null ? "Enregistrer" : "Mettre à jour");
        Button cancelBtn = createOutlineButton("Annuler");
        cancelBtn.setOnAction(e -> popup.close());
        saveBtn.setOnAction(e -> {
            if (tfReviewer.getText().isBlank() || tfProductId.getText().isBlank())
                return;
            try {
                Review rev = existing != null ? existing : new Review();
                rev.setReviewerName(tfReviewer.getText().trim());
                rev.setRating(cbRating.getValue());
                rev.setComment(taComment.getText().trim());
                rev.setProductId(Integer.parseInt(tfProductId.getText().trim()));
                if (existing == null)
                    service.add(rev);
                else
                    service.update(rev);
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
                "-fx-background-color: #c0c0c0; -fx-text-fill: #1a1a1a; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
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

    private TextArea styledArea(String val) {
        TextArea ta = new TextArea(val);
        ta.setStyle("-fx-control-inner-background: #2a2a2a; -fx-background-color: #2a2a2a; " +
                "-fx-text-fill: #ffffff; -fx-prompt-text-fill: #666; " +
                "-fx-border-color: #444; -fx-border-radius: 5; -fx-background-radius: 5;");
        ta.setPrefWidth(260);
        ta.setPrefRowCount(3);
        return ta;
    }

    private Label fl(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12;");
        l.setMinWidth(110);
        return l;
    }
}
