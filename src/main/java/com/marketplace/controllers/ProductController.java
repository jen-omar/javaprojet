package com.marketplace.controllers;

import com.marketplace.models.Product;
import com.marketplace.services.ProductService;
import com.marketplace.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class ProductController {

    private final ProductService service = new ProductService();
    private final SessionManager session = SessionManager.getInstance();

    // ── Main View ─────────────────────────────────────────────────
    public Node buildView() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1a1a1a;");

        Label title = new Label(session.isArtist() ? "MES PRODUITS" : "PRODUITS");
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 22; -fx-font-weight: bold;");
        Label subtitle = new Label(session.isArtist()
                ? "Vos œuvres d'art — consultez et modifiez vos informations"
                : "Gérer toutes les œuvres d'art du marketplace");
        subtitle.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");
        VBox titleBox = new VBox(2, title, subtitle);

        Button addBtn = createGoldButton("+ Ajouter un produit");
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
        refreshCards(cards);

        root.getChildren().addAll(headerRow, cards);
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1a1a1a; -fx-background-color: #1a1a1a; -fx-border-color: transparent;");
        return scroll;
    }

    // ── Artist-filtered refresh ───────────────────────────────────
    private void refreshCards(FlowPane cards) {
        cards.getChildren().clear();
        List<Product> products = service.getAll();
        if (session.isArtist()) {
            products = products.stream()
                    .filter(p -> session.getName().equalsIgnoreCase(p.getArtistName() != null ? p.getArtistName() : ""))
                    .collect(Collectors.toList());
        }
        if (products.isEmpty()) {
            Label empty = new Label(session.isArtist()
                    ? "Vous n'avez aucun produit publié. Cliquez «+ Ajouter» pour commencer."
                    : "Aucun produit.");
            empty.setStyle("-fx-text-fill: #666; -fx-font-size: 13;");
            cards.getChildren().add(empty);
            return;
        }
        for (Product p : products)
            cards.getChildren().add(buildCard(p, cards));
    }

    // ── Card ──────────────────────────────────────────────────────
    private Node buildCard(Product p, FlowPane cards) {
        VBox card = new VBox(0);
        card.setPrefWidth(285);
        card.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10; " +
                "-fx-border-color: #333; -fx-border-radius: 10; -fx-border-width: 1;");

        String rawUrl = p.getImageUrl();
        if (rawUrl != null && !rawUrl.isBlank()) {
            try {
                String url = rawUrl.startsWith("http") || rawUrl.startsWith("file:") ? rawUrl
                        : "http://localhost" + rawUrl;
                Image img = new Image(url, 285, 155, false, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(285);
                iv.setFitHeight(155);
                iv.setPreserveRatio(false);
                img.errorProperty().addListener((obs, old, err) -> {
                    if (err)
                        card.getChildren().remove(iv);
                });
                card.getChildren().add(iv);
            } catch (Exception ignored) {
            }
        }

        VBox content = new VBox(8);
        content.setPadding(new Insets(14, 16, 16, 16));

        Label name = new Label(p.getName());
        name.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 15;");
        name.setWrapText(true);

        Label sub = new Label("Par " + p.getArtistName() + "   ·   " + p.getPrice() + " €");
        sub.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 12;");

        String d = p.getDescription() != null && !p.getDescription().isBlank() ? p.getDescription()
                : "Aucune description.";
        Label desc = new Label(d.length() > 65 ? d.substring(0, 65) + "…" : d);
        desc.setStyle("-fx-text-fill: #777; -fx-font-size: 12;");
        desc.setWrapText(true);

        HBox tags = new HBox(6);
        tags.getChildren().addAll(tag(p.getType()), tag(p.getSaleType()), statusTag(p.getStatus()));

        HBox buttons = new HBox(8);
        Button editBtn = createOutlineButton("✎ Éditer");
        editBtn.setOnAction(e -> showForm(p, cards));
        buttons.getChildren().add(editBtn);

        Button deleteBtn = createDeleteButton("✕ Supprimer");
        deleteBtn.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer \"" + p.getName() + "\" ?", ButtonType.YES,
                    ButtonType.NO);
            a.setHeaderText(null);
            a.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    service.delete(p.getId());
                    refreshCards(cards);
                }
            });
        });
        buttons.getChildren().add(deleteBtn);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        content.getChildren().addAll(name, sub, desc, tags, spacer, buttons);
        card.getChildren().add(content);
        return card;
    }

    // ── Form (Add / Edit) ─────────────────────────────────────────
    private void showForm(Product existing, FlowPane cards) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle(existing == null ? "Ajouter un produit" : "Modifier le produit");
        popup.setWidth(550);
        popup.setHeight(700);

        if (session.isArtist()) {
            showArtistForm(existing, cards, popup);
        } else {
            showAdminForm(existing, cards, popup);
        }
    }

    // ── Validation: live border-color feedback while typing ──────
    private void applyValidation(TextField field, Label error, String msg) {
        field.textProperty().addListener((obs, old, val) -> {
            boolean invalid = val.trim().isBlank();
            if (invalid) {
                setFieldErrorStyle(field);
                error.setText(msg);
            } else {
                clearFieldStyle(field);
                error.setText("");
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    // ── ARTIST Form ──────────────────────────────────────────────
    // ════════════════════════════════════════════════════════════
    private void showArtistForm(Product existing, FlowPane cards, Stage popup) {
        GridPane grid = mkGrid();

        // Fields
        TextField tfName  = sf(existing != null ? existing.getName() : "");
        tfName.setPromptText("ex: Mon tableau abstrait");
        TextField tfPrice = sf(existing != null && existing.getPrice() != null
                ? existing.getPrice().toPlainString() : "");
        tfPrice.setPromptText("ex: 120.00");
        TextArea taDesc = sa(existing != null && existing.getDescription() != null
                ? existing.getDescription() : "");
        taDesc.setPromptText("Description (optionnel, max 500 caractères)");
        TextField tfImage = sf(existing != null && existing.getImageUrl() != null
                ? existing.getImageUrl() : "");
        tfImage.setPromptText("URL ou chemin fichier");

        // Inline error labels
        Label errName  = errLbl();
        Label errPrice = errLbl();
        Label errDesc  = errLbl();
        Label errImage = errLbl();

        // Live validation
        applyValidation(tfName, errName, "Nom requis");

        // Description character counter
        Label descCounter = counterLbl("0 / 500");
        taDesc.textProperty().addListener((obs, old, val) -> {
            int len = val.length();
            descCounter.setText(len + " / 500");
            descCounter.setStyle("-fx-font-size: 10; -fx-text-fill: " + (len > 500 ? "#ff6b6b" : "#555") + ";");
        });

        // Image browse
        Button browseBtn = createOutlineButton("...");
        browseBtn.setOnAction(e -> browseImage(tfImage, popup));
        HBox imageRow = new HBox(8, tfImage, browseBtn);
        HBox.setHgrow(tfImage, Priority.ALWAYS);

        // Dropdowns
        ComboBox<String> cbType = sc("Digital Art", "Painting", "Sculpture", "Drawing", "Photography", "Mixed Media", "Other");
        ComboBox<String> cbCat  = sc("Still Life", "Landscape", "Portrait", "Modern", "Minimalist", "Surrealist", "Pop Art", "Classical", "Impressionist");
        ComboBox<String> cbSale = sc("fixed", "auction");
        ComboBox<String> cbStat = sc("available", "sold", "annulee");
        if (existing != null) {
            cbType.setValue(existing.getType()); cbCat.setValue(existing.getCategory());
            cbSale.setValue(existing.getSaleType()); cbStat.setValue(existing.getStatus());
        }

        // Note label
        Label note = new Label("\u24d8 Artiste et IDs sont gérés par l'administrateur.");
        note.setStyle("-fx-text-fill: #555; -fx-font-size: 11;");

        int row = 0;
        grid.add(fl("Nom *"),         0, row); grid.add(tfName,                  1, row); grid.add(errName,  2, row++);
        grid.add(fl("Prix (\u20AC) *"), 0, row); grid.add(tfPrice,                 1, row); grid.add(errPrice, 2, row++);
        VBox descBox = new VBox(2, taDesc, descCounter);
        grid.add(fl("Description"),   0, row); grid.add(descBox,                  1, row); grid.add(errDesc,  2, row++);
        grid.add(fl("Type"),          0, row); grid.add(cbType,                   1, row++);
        grid.add(fl("Catégorie"),     0, row); grid.add(cbCat,                    1, row++);
        grid.add(fl("Type de vente"), 0, row); grid.add(cbSale,                   1, row++);
        grid.add(fl("Statut"),        0, row); grid.add(cbStat,                   1, row++);
        grid.add(fl("Image"),         0, row); grid.add(imageRow,                 1, row); grid.add(errImage, 2, row++);
        grid.add(note,                0, row, 3, 1);

        // Error summary
        Label errorSummary = new Label("");
        errorSummary.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11;");
        errorSummary.setWrapText(true);

        Button saveBtn   = createGoldButton(existing == null ? "Enregistrer" : "Mettre à jour");
        Button cancelBtn = createOutlineButton("Annuler");
        cancelBtn.setOnAction(e -> popup.close());

        saveBtn.setOnAction(e -> {
            // Reset all errors
            errName.setText(""); errPrice.setText(""); errDesc.setText(""); errImage.setText("");
            clearFieldStyle(tfName); clearFieldStyle(tfPrice); clearFieldStyle(taDesc); clearFieldStyle(tfImage);
            errorSummary.setText("");
            boolean valid = true;

            // Nom
            String nom = tfName.getText().trim();
            if (nom.isBlank()) {
                setFieldErrorStyle(tfName); errName.setText("Requis"); valid = false;
            } else if (nom.length() < 2) {
                setFieldErrorStyle(tfName); errName.setText("Min 2 car."); valid = false;
            } else if (nom.length() > 100) {
                setFieldErrorStyle(tfName); errName.setText("Max 100 car."); valid = false;
            }

            // Prix
            BigDecimal price = null;
            String priceStr = tfPrice.getText().trim();
            if (priceStr.isBlank()) {
                setFieldErrorStyle(tfPrice); errPrice.setText("Requis"); valid = false;
            } else {
                try {
                    price = new BigDecimal(priceStr);
                    if (price.compareTo(BigDecimal.ZERO) <= 0) {
                        setFieldErrorStyle(tfPrice); errPrice.setText("> 0 svp"); valid = false;
                    }
                } catch (NumberFormatException ex) {
                    setFieldErrorStyle(tfPrice); errPrice.setText("Invalide"); valid = false;
                }
            }

            // Description
            String descText = taDesc.getText().trim();
            if (descText.length() > 500) {
                setFieldErrorStyle(taDesc); errDesc.setText("Max 500 car."); valid = false;
            }

            // Image URL (optional)
            String imgUrl = tfImage.getText().trim();
            if (!imgUrl.isBlank() && !imgUrl.startsWith("http") && !imgUrl.startsWith("file:") && !imgUrl.startsWith("/")) {
                setFieldErrorStyle(tfImage); errImage.setText("URL invalide"); valid = false;
            }

            if (!valid) {
                errorSummary.setText("\u26a0 Corrigez les champs en rouge.");
                return;
            }

            Product p = existing != null ? existing : new Product();
            p.setName(nom);
            if (existing == null) p.setArtistName(session.getName());
            p.setPrice(price);
            p.setDescription(descText);
            p.setType(cbType.getValue()); p.setCategory(cbCat.getValue());
            p.setSaleType(cbSale.getValue()); p.setStatus(cbStat.getValue());
            p.setImageUrl(imgUrl);
            if (existing == null) service.add(p); else service.update(p);
            popup.close(); refreshCards(cards);
        });

        HBox btnRow = new HBox(10, saveBtn, cancelBtn);
        btnRow.setPadding(new Insets(4, 28, 20, 28));
        errorSummary.setPadding(new Insets(0, 28, 0, 28));
        ScrollPane scroll = new ScrollPane(new VBox(8, grid, errorSummary, btnRow));
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1a1a1a; -fx-background-color: #1a1a1a; -fx-border-color: transparent;");
        VBox layout = new VBox(0, scroll);
        layout.setStyle("-fx-background-color: #1a1a1a;");
        popup.setScene(new Scene(layout)); popup.show();
    }

    // ════════════════════════════════════════════════════════════
    // ── ADMIN Form ───────────────────────────────────────────────
    // ════════════════════════════════════════════════════════════
    private void showAdminForm(Product existing, FlowPane cards, Stage popup) {
        GridPane grid = mkGrid();

        // Fields
        TextField tfName   = sf(existing != null ? existing.getName() : "");
        tfName.setPromptText("ex: Tableau impressionniste");
        TextField tfArtist = sf(existing != null && existing.getArtistName() != null ? existing.getArtistName() : "");
        tfArtist.setPromptText("Nom complet de l'artiste");
        TextField tfPrice  = sf(existing != null && existing.getPrice() != null
                ? existing.getPrice().toPlainString() : "");
        tfPrice.setPromptText("ex: 250.00");
        TextArea taDesc = sa(existing != null && existing.getDescription() != null ? existing.getDescription() : "");
        taDesc.setPromptText("Description (optionnel, max 500 caractères)");
        TextField tfImage = sf(existing != null && existing.getImageUrl() != null ? existing.getImageUrl() : "");
        tfImage.setPromptText("URL ou chemin fichier image");

        // Inline error labels
        Label errName   = errLbl();
        Label errArtist = errLbl();
        Label errPrice  = errLbl();
        Label errDesc   = errLbl();
        Label errImage  = errLbl();

        // Live validation
        applyValidation(tfName, errName, "Nom requis");
        applyValidation(tfArtist, errArtist, "Artiste requis");

        // Description character counter
        Label descCounter = counterLbl("0 / 500");
        taDesc.textProperty().addListener((obs, old, val) -> {
            int len = val.length();
            descCounter.setText(len + " / 500");
            descCounter.setStyle("-fx-font-size: 10; -fx-text-fill: " + (len > 500 ? "#ff6b6b" : "#555") + ";");
        });

        // Image browse
        Button browseBtn = createOutlineButton("...");
        browseBtn.setOnAction(e -> browseImage(tfImage, popup));
        HBox imageRow = new HBox(8, tfImage, browseBtn);
        HBox.setHgrow(tfImage, Priority.ALWAYS);

        // Dropdowns
        ComboBox<String> cbType = sc("Digital Art", "Painting", "Sculpture", "Drawing", "Photography", "Mixed Media", "Other");
        ComboBox<String> cbCat  = sc("Still Life", "Landscape", "Portrait", "Modern", "Minimalist", "Surrealist", "Pop Art", "Classical", "Impressionist");
        ComboBox<String> cbSale = sc("fixed", "auction");
        ComboBox<String> cbStat = sc("available", "sold", "annulee");
        if (existing != null) {
            cbType.setValue(existing.getType()); cbCat.setValue(existing.getCategory());
            cbSale.setValue(existing.getSaleType()); cbStat.setValue(existing.getStatus());
        }

        int row = 0;
        grid.add(fl("Nom *"),          0, row); grid.add(tfName,    1, row); grid.add(errName,   2, row++);
        grid.add(fl("Artiste *"),      0, row); grid.add(tfArtist,  1, row); grid.add(errArtist, 2, row++);
        grid.add(fl("Prix (\u20AC) *"), 0, row); grid.add(tfPrice,   1, row); grid.add(errPrice,  2, row++);
        VBox descBox2 = new VBox(2, taDesc, descCounter);
        grid.add(fl("Description"),    0, row); grid.add(descBox2,  1, row); grid.add(errDesc,   2, row++);
        grid.add(fl("Type"),           0, row); grid.add(cbType,    1, row++);
        grid.add(fl("Catégorie"),      0, row); grid.add(cbCat,     1, row++);
        grid.add(fl("Type de vente"),  0, row); grid.add(cbSale,    1, row++);
        grid.add(fl("Statut"),         0, row); grid.add(cbStat,    1, row++);
        grid.add(fl("Image (URL)"),    0, row); grid.add(imageRow,  1, row); grid.add(errImage,  2, row++);

        // Error summary
        Label errorSummary = new Label("");
        errorSummary.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11;");
        errorSummary.setWrapText(true);

        Button saveBtn   = createGoldButton(existing == null ? "Enregistrer" : "Mettre à jour");
        Button cancelBtn = createOutlineButton("Annuler");
        cancelBtn.setOnAction(e -> popup.close());

        saveBtn.setOnAction(e -> {
            // Reset all errors
            errName.setText(""); errArtist.setText(""); errPrice.setText("");
            errDesc.setText(""); errImage.setText("");
            clearFieldStyle(tfName); clearFieldStyle(tfArtist); clearFieldStyle(tfPrice);
            clearFieldStyle(taDesc); clearFieldStyle(tfImage);
            errorSummary.setText("");
            boolean valid = true;

            // Nom
            String nom = tfName.getText().trim();
            if (nom.isBlank()) {
                setFieldErrorStyle(tfName); errName.setText("Requis"); valid = false;
            } else if (nom.length() < 2) {
                setFieldErrorStyle(tfName); errName.setText("Min 2 car."); valid = false;
            } else if (nom.length() > 100) {
                setFieldErrorStyle(tfName); errName.setText("Max 100 car."); valid = false;
            }

            // Artiste
            String artist = tfArtist.getText().trim();
            if (artist.isBlank()) {
                setFieldErrorStyle(tfArtist); errArtist.setText("Requis"); valid = false;
            } else if (artist.length() < 2) {
                setFieldErrorStyle(tfArtist); errArtist.setText("Min 2 car."); valid = false;
            } else if (artist.length() > 80) {
                setFieldErrorStyle(tfArtist); errArtist.setText("Max 80 car."); valid = false;
            }

            // Prix
            BigDecimal price = null;
            String priceStr = tfPrice.getText().trim();
            if (priceStr.isBlank()) {
                setFieldErrorStyle(tfPrice); errPrice.setText("Requis"); valid = false;
            } else {
                try {
                    price = new BigDecimal(priceStr);
                    if (price.compareTo(BigDecimal.ZERO) <= 0) {
                        setFieldErrorStyle(tfPrice); errPrice.setText("> 0 svp"); valid = false;
                    } else if (price.compareTo(new BigDecimal("999999")) > 0) {
                        setFieldErrorStyle(tfPrice); errPrice.setText("Trop élevé"); valid = false;
                    }
                } catch (NumberFormatException ex) {
                    setFieldErrorStyle(tfPrice); errPrice.setText("Invalide"); valid = false;
                }
            }

            // Description length
            String descText = taDesc.getText().trim();
            if (descText.length() > 500) {
                setFieldErrorStyle(taDesc); errDesc.setText("Max 500 car."); valid = false;
            }

            // Image URL (optional)
            String imgUrl = tfImage.getText().trim();
            if (!imgUrl.isBlank() && !imgUrl.startsWith("http") && !imgUrl.startsWith("file:") && !imgUrl.startsWith("/")) {
                setFieldErrorStyle(tfImage); errImage.setText("URL invalide"); valid = false;
            }

            if (!valid) {
                errorSummary.setText("\u26a0 Corrigez les champs en rouge.");
                return;
            }

            Product p = existing != null ? existing : new Product();
            p.setName(nom); p.setArtistName(artist); p.setPrice(price);
            p.setDescription(descText); p.setType(cbType.getValue()); p.setCategory(cbCat.getValue());
            p.setSaleType(cbSale.getValue()); p.setStatus(cbStat.getValue()); p.setImageUrl(imgUrl);
            if (existing == null) service.add(p); else service.update(p);
            popup.close(); refreshCards(cards);
        });

        HBox btnRow = new HBox(10, saveBtn, cancelBtn);
        btnRow.setPadding(new Insets(4, 28, 20, 28));
        errorSummary.setPadding(new Insets(0, 28, 0, 28));
        ScrollPane scroll = new ScrollPane(new VBox(8, grid, errorSummary, btnRow));
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1a1a1a; -fx-background-color: #1a1a1a; -fx-border-color: transparent;");
        VBox layout = new VBox(0, scroll);
        layout.setStyle("-fx-background-color: #1a1a1a;");
        popup.setScene(new Scene(layout)); popup.show();
    }

    private void browseImage(TextField target, Stage owner) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File f = fc.showOpenDialog(owner);
        if (f != null) target.setText(f.toURI().toString());
    }

    private GridPane mkGrid() {
        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(14); g.setPadding(new Insets(24, 28, 12, 28));
        ColumnConstraints c0 = new ColumnConstraints(100), c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c0, c1, new ColumnConstraints(80));
        return g;
    }

    private Label errLbl() {
        Label l = new Label("");
        l.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 10;");
        l.setWrapText(true);
        return l;
    }

    private Label counterLbl(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 10; -fx-text-fill: #555;");
        return l;
    }

    /** Apply red border to a field to indicate error */
    private void setFieldErrorStyle(Control field) {
        if (field instanceof TextField) {
            field.setStyle("-fx-control-inner-background: #2a2a2a; -fx-background-color: #2a2a2a; " +
                    "-fx-text-fill: #ffffff; -fx-border-color: #ff6b6b; -fx-border-width: 1.5; " +
                    "-fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");
        } else if (field instanceof TextArea) {
            field.setStyle("-fx-control-inner-background: #2a2a2a; -fx-background-color: #2a2a2a; " +
                    "-fx-text-fill: #ffffff; -fx-border-color: #ff6b6b; -fx-border-width: 1.5; " +
                    "-fx-border-radius: 5; -fx-background-radius: 5;");
        }
    }

    /** Remove error style, revert to normal */
    private void clearFieldStyle(Control field) {
        if (field instanceof TextField) {
            field.setStyle("-fx-control-inner-background: #2a2a2a; -fx-background-color: #2a2a2a; " +
                    "-fx-text-fill: #ffffff; -fx-border-color: #444; " +
                    "-fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");
        } else if (field instanceof TextArea) {
            field.setStyle("-fx-control-inner-background: #2a2a2a; -fx-background-color: #2a2a2a; " +
                    "-fx-text-fill: #ffffff; -fx-border-color: #444; " +
                    "-fx-border-radius: 5; -fx-background-radius: 5;");
        }
    }

    private Button createGoldButton(String t) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color: #c0c0c0; -fx-text-fill: #111; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 18 8 18; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #dcdcdc; -fx-text-fill: #111; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 18 8 18; -fx-cursor: hand;"));
        b.setOnMouseExited(e  -> b.setStyle("-fx-background-color: #c0c0c0; -fx-text-fill: #111; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 18 8 18; -fx-cursor: hand;"));
        return b;
    }

    private Button createOutlineButton(String t) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccc; -fx-border-color: #555; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #333; -fx-text-fill: #fff; -fx-border-color: #888; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 14 6 14; -fx-cursor: hand;"));
        b.setOnMouseExited(e  -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccc; -fx-border-color: #555; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 14 6 14; -fx-cursor: hand;"));
        return b;
    }

    private Button createDeleteButton(String t) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; -fx-border-color: #444; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #4a1a1a; -fx-text-fill: #ff6b6b; -fx-border-color: #8b3a3a; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 14 6 14; -fx-cursor: hand;"));
        b.setOnMouseExited(e  -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; -fx-border-color: #444; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 14 6 14; -fx-cursor: hand;"));
        return b;
    }

    private Label tag(String text) {
        Label l = new Label(text != null ? text : "—");
        l.setStyle("-fx-background-color: #333; -fx-text-fill: #aaa; -fx-font-size: 10; -fx-background-radius: 4; -fx-padding: 2 6;");
        return l;
    }

    private Label statusTag(String s) {
        String c = "available".equals(s) ? "#2d6a2d" : "sold".equals(s) ? "#6a2d2d" : "#444";
        Label l = new Label("available".equals(s) ? "Disponible" : "sold".equals(s) ? "Vendu" : s);
        l.setStyle("-fx-background-color: " + c + "; -fx-text-fill: #ccc; -fx-font-size: 10; -fx-background-radius: 4; -fx-padding: 2 6;");
        return l;
    }

    private TextField sf(String val) {
        TextField tf = new TextField(val);
        // Using -fx-background-color: -fx-control-inner-background makes
        // -fx-prompt-text-fill work properly in JavaFX inline CSS
        tf.setStyle(
                "-fx-control-inner-background: #2a2a2a; " +
                "-fx-background-color: -fx-control-inner-background; " +
                "-fx-text-fill: #ffffff; " +
                "-fx-prompt-text-fill: derive(-fx-control-inner-background, +80%); " +
                "-fx-border-color: #444; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");
        // Gold focus ring
        tf.focusedProperty().addListener((obs, wasF, isF) -> {
            boolean hasError = tf.getStyle().contains("#ff6b6b");
            if (!hasError) {
                String base = "-fx-control-inner-background: #2a2a2a; " +
                        "-fx-background-color: -fx-control-inner-background; " +
                        "-fx-text-fill: #ffffff; " +
                        "-fx-prompt-text-fill: derive(-fx-control-inner-background, +80%); ";
                tf.setStyle(isF
                    ? base + "-fx-border-color: #c0c0c0; -fx-border-width: 1.5; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;"
                    : base + "-fx-border-color: #444; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");
            }
        });
        return tf;
    }

    private TextArea sa(String val) {
        TextArea ta = new TextArea(val);
        ta.setStyle(
                "-fx-control-inner-background: #2a2a2a; " +
                "-fx-background-color: -fx-control-inner-background; " +
                "-fx-text-fill: #ffffff; " +
                "-fx-prompt-text-fill: derive(-fx-control-inner-background, +80%); " +
                "-fx-border-color: #444; -fx-border-radius: 5; -fx-background-radius: 5;");
        ta.setPrefRowCount(3);
        ta.focusedProperty().addListener((obs, wasF, isF) -> {
            boolean hasError = ta.getStyle().contains("#ff6b6b");
            if (!hasError) {
                String base = "-fx-control-inner-background: #2a2a2a; " +
                        "-fx-background-color: -fx-control-inner-background; " +
                        "-fx-text-fill: #ffffff; " +
                        "-fx-prompt-text-fill: derive(-fx-control-inner-background, +80%); ";
                ta.setStyle(isF
                    ? base + "-fx-border-color: #c0c0c0; -fx-border-width: 1.5; -fx-border-radius: 5; -fx-background-radius: 5;"
                    : base + "-fx-border-color: #444; -fx-border-radius: 5; -fx-background-radius: 5;");
            }
        });
        return ta;
    }

    private ComboBox<String> sc(String... items) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll(items);
        cb.setPrefWidth(260);
        if (items.length > 0) cb.setValue(items[0]);
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

    private Label fl(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12;");
        l.setMinWidth(110);
        return l;
    }
}
