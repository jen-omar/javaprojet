package com.marketplace.controllers;

import com.marketplace.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;

/**
 * LoginController — 3 roles: Admin / Artiste / Client.
 * Admin  → accès complet (CRUD)
 * Artiste→ gérer ses propres produits
 * Client → parcourir et acheter des produits
 */
public class LoginController {

    private String selectedRole = "admin";
    private Button btnAdmin, btnArtist, btnClient;

    public VBox buildView(Runnable onLogin) {
        VBox root = new VBox(0);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #111111;");

        // ── Brand ────────────────────────────────────────────────────
        VBox brand = new VBox(6);
        brand.setAlignment(Pos.CENTER);
        brand.setPadding(new Insets(50, 0, 36, 0));

        Label title = new Label("MARKETPLACE");
        title.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 28; -fx-font-weight: bold; -fx-letter-spacing: 3;");
        Label sub = new Label("Art & Collection · Connexion");
        sub.setStyle("-fx-text-fill: #444; -fx-font-size: 12;");
        brand.getChildren().addAll(title, sub);

        // ── Card ─────────────────────────────────────────────────────
        VBox card = new VBox(20);
        card.setPadding(new Insets(32, 36, 36, 36));
        card.setMaxWidth(400);
        card.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 12; " +
                "-fx-border-color: #2e2e2e; -fx-border-radius: 12; -fx-border-width: 1;");

        // ── Role toggle (3 buttons) ───────────────────────────────────
        Label roleLabel = new Label("Votre rôle");
        roleLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");

        HBox toggle = new HBox(0);
        toggle.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 8;");
        toggle.setPrefHeight(42);

        btnAdmin  = toggleBtn("Admin",   true);
        btnArtist = toggleBtn("Artiste", false);
        btnClient = toggleBtn("Client",  false);

        HBox.setHgrow(btnAdmin,  Priority.ALWAYS);
        HBox.setHgrow(btnArtist, Priority.ALWAYS);
        HBox.setHgrow(btnClient, Priority.ALWAYS);
        toggle.getChildren().addAll(btnAdmin, btnArtist, btnClient);

        // ── Name field (Artist & Client) ──────────────────────────────
        VBox nameBox = new VBox(6);
        Label nameLabel = new Label("Votre nom *");
        nameLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12;");

        TextField tfName = styledField("Ex: Omar, Maria, Aziz…");
        nameBox.getChildren().addAll(nameLabel, tfName);
        nameBox.setVisible(false);
        nameBox.setManaged(false);

        // ── Role switching logic ──────────────────────────────────────
        btnAdmin.setOnAction(e -> {
            selectedRole = "admin";
            activateToggle(btnAdmin, btnArtist, btnClient);
            nameBox.setVisible(false);
            nameBox.setManaged(false);
            nameLabel.setText("Votre nom *");
        });
        btnArtist.setOnAction(e -> {
            selectedRole = "artist";
            activateToggle(btnArtist, btnAdmin, btnClient);
            nameLabel.setText("Votre nom d'artiste *");
            nameBox.setVisible(true);
            nameBox.setManaged(true);
        });
        btnClient.setOnAction(e -> {
            selectedRole = "client";
            activateToggle(btnClient, btnAdmin, btnArtist);
            nameLabel.setText("Votre prénom *");
            nameBox.setVisible(true);
            nameBox.setManaged(true);
        });

        // ── Error label ───────────────────────────────────────────────
        Label errorLbl = new Label("");
        errorLbl.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12;");
        errorLbl.setVisible(false);
        errorLbl.setManaged(false);

        // ── Login button ──────────────────────────────────────────────
        Button loginBtn = new Button("Se connecter →");
        loginBtn.setPrefWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(46);
        loginBtn.setStyle("-fx-background-color: #c0c0c0; -fx-text-fill: #111111; " +
                "-fx-font-weight: bold; -fx-font-size: 14; -fx-background-radius: 8; -fx-cursor: hand;");
        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle(
                "-fx-background-color: #dcdcdc; -fx-text-fill: #111111; " +
                        "-fx-font-weight: bold; -fx-font-size: 14; -fx-background-radius: 8; -fx-cursor: hand;"));
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle(
                "-fx-background-color: #c0c0c0; -fx-text-fill: #111111; " +
                        "-fx-font-weight: bold; -fx-font-size: 14; -fx-background-radius: 8; -fx-cursor: hand;"));

        loginBtn.setOnAction(e -> {
            SessionManager session = SessionManager.getInstance();
            errorLbl.setVisible(false);
            errorLbl.setManaged(false);

            if ("artist".equals(selectedRole) || "client".equals(selectedRole)) {
                String name = tfName.getText().trim();
                if (name.isBlank()) {
                    tfName.setStyle(tfName.getStyle()
                            .replace("-fx-border-color: #444;", "-fx-border-color: #ff6b6b; -fx-border-width: 1.5;"));
                    errorLbl.setText("Veuillez entrer votre nom pour continuer.");
                    errorLbl.setVisible(true);
                    errorLbl.setManaged(true);
                    return;
                }
                if (name.length() < 2) {
                    errorLbl.setText("Le nom doit contenir au moins 2 caractères.");
                    errorLbl.setVisible(true);
                    errorLbl.setManaged(true);
                    return;
                }
                session.setRole(selectedRole);
                session.setName(name);
            } else {
                session.setRole("admin");
                session.setName("Admin");
            }
            onLogin.run();
        });

        // ── Role description hints ────────────────────────────────────
        Label hint = roleHint();

        card.getChildren().addAll(roleLabel, toggle, nameBox, errorLbl, loginBtn);

        root.getChildren().addAll(brand, card, hint);
        VBox.setVgrow(root, Priority.ALWAYS);
        return root;
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        // Use -fx-control-inner-background for prompt text visibility
        tf.setStyle(
                "-fx-control-inner-background: #2a2a2a; " +
                "-fx-background-color: -fx-control-inner-background; " +
                "-fx-text-fill: #ffffff; " +
                "-fx-prompt-text-fill: derive(-fx-control-inner-background, +80%); " +
                "-fx-border-color: #444; -fx-border-radius: 6; -fx-background-radius: 6; " +
                "-fx-padding: 10; -fx-font-size: 13;");
        tf.focusedProperty().addListener((obs, wasF, isF) -> {
            if (isF) {
                tf.setStyle(
                        "-fx-control-inner-background: #2a2a2a; " +
                        "-fx-background-color: -fx-control-inner-background; " +
                        "-fx-text-fill: #ffffff; " +
                        "-fx-prompt-text-fill: derive(-fx-control-inner-background, +80%); " +
                        "-fx-border-color: #c0c0c0; -fx-border-width: 1.5; " +
                        "-fx-border-radius: 6; -fx-background-radius: 6; " +
                        "-fx-padding: 10; -fx-font-size: 13;");
            } else {
                tf.setStyle(
                        "-fx-control-inner-background: #2a2a2a; " +
                        "-fx-background-color: -fx-control-inner-background; " +
                        "-fx-text-fill: #ffffff; " +
                        "-fx-prompt-text-fill: derive(-fx-control-inner-background, +80%); " +
                        "-fx-border-color: #444; -fx-border-radius: 6; -fx-background-radius: 6; " +
                        "-fx-padding: 10; -fx-font-size: 13;");
            }
        });
        return tf;
    }

    private Button toggleBtn(String label, boolean active) {
        Button b = new Button(label);
        b.setPrefHeight(42);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle(active ? activeToggleStyle() : inactiveToggleStyle());
        return b;
    }

    private void activateToggle(Button on, Button... offs) {
        on.setStyle(activeToggleStyle());
        for (Button off : offs) off.setStyle(inactiveToggleStyle());
    }

    private String activeToggleStyle() {
        return "-fx-background-color: #c0c0c0; -fx-text-fill: #111111; -fx-font-weight: bold; " +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 13;";
    }

    private String inactiveToggleStyle() {
        return "-fx-background-color: #2a2a2a; -fx-text-fill: #777777; " +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 13;";
    }

    private Label roleHint() {
        Label l = new Label(
                "Admin = accès complet  ·  Artiste = ses produits  ·  Client = parcourir & acheter");
        l.setStyle("-fx-text-fill: #333; -fx-font-size: 11;");
        l.setTextAlignment(TextAlignment.CENTER);
        l.setPadding(new Insets(16, 0, 0, 0));
        return l;
    }
}
