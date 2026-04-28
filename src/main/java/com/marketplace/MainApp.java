package com.marketplace;

import com.marketplace.controllers.*;
import com.marketplace.util.SessionManager;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * MainApp — entry point.
 * Roles: admin (full access) | artist (own products) | client (browse + buy)
 */
public class MainApp extends Application {

    private Stage primaryStage;

    // ── Controllers ───────────────────────────────────────────────
    private HomeController     homeController;
    private ProductController  productController;
    private OrderController    orderController;
    private BidController      bidController;
    private ReviewController   reviewController;
    private WishlistController wishlistController;
    private ClientController   clientController;

    // ── Layout refs ───────────────────────────────────────────────
    private StackPane contentArea;
    private HBox  activeNavItem  = null;
    private Label activeNavIcon  = null;
    private Label activeNavLabel = null;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showLoginScreen();
    }

    // ── Login ─────────────────────────────────────────────────────
    private void showLoginScreen() {
        LoginController loginCtrl = new LoginController();
        Scene loginScene = new Scene(loginCtrl.buildView(this::onLoginSuccess), 480.0, 590.0);
        try {
            String css = getClass().getResource("/styles/dark-silver.css").toExternalForm();
            loginScene.getStylesheets().add(css);
        } catch (Exception ignored) {}
        primaryStage.setTitle("MARKETPLACE — Connexion");
        primaryStage.setScene(loginScene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // ── After login ───────────────────────────────────────────────
    private void onLoginSuccess() {
        homeController     = new HomeController();
        productController  = new ProductController();
        orderController    = new OrderController();
        bidController      = new BidController();
        reviewController   = new ReviewController();
        wishlistController = new WishlistController();
        clientController   = new ClientController();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a1a;");
        root.setLeft(buildSidebar());

        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #1a1a1a;");
        root.setCenter(contentArea);

        showSection(SessionManager.getInstance().isClient() ? "catalogue" : "home");

        Scene mainScene = new Scene(root, 1050, 700);
        try {
            String css = getClass().getResource("/styles/dark-silver.css").toExternalForm();
            mainScene.getStylesheets().add(css);
        } catch (Exception ignored) {}

        primaryStage.setTitle("MARKETPLACE");
        primaryStage.setScene(mainScene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(550);
    }

    // ── Section switching ─────────────────────────────────────────
    private void showSection(String section) {
        contentArea.getChildren().clear();
        Node view = switch (section) {
            case "home"      -> homeController.buildView();
            case "products"  -> productController.buildView();
            case "orders"    -> orderController.buildView();
            case "bids"      -> bidController.buildView();
            case "reviews"   -> reviewController.buildView();
            case "wishlist"  -> wishlistController.buildView();
            case "catalogue" -> clientController.buildView();
            default          -> homeController.buildView();
        };
        contentArea.getChildren().add(view);
    }

    // ── Sidebar — role-based ──────────────────────────────────────
    private VBox buildSidebar() {
        SessionManager session = SessionManager.getInstance();
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(210);
        sidebar.setStyle("-fx-background-color: #111111;");

        // Brand
        VBox brand = new VBox(4);
        brand.setPadding(new Insets(28, 20, 20, 20));
        
        javafx.scene.image.ImageView logoView = new javafx.scene.image.ImageView(
                new javafx.scene.image.Image(getClass().getResourceAsStream("/images/logo.jpg"))
        );
        logoView.setFitWidth(150);
        logoView.setPreserveRatio(true);

        Label brandSub = new Label("Art & Collection");
        brandSub.setStyle("-fx-text-fill: #444; -fx-font-size: 11;");
        brand.getChildren().addAll(logoView, brandSub);

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #222;");

        // Nav
        VBox nav = new VBox(0);
        nav.setPadding(new Insets(12, 0, 12, 0));

        if (session.isClient()) {
            // Client: Catalogue + Avis + Liste de souhaits (sans Accueil)
            HBox[] items = {
                    buildNavItem("🛍", "CATALOGUE",         "catalogue"),
                    buildNavItem("★",  "AVIS",              "reviews"),
                    buildNavItem("♥",  "LISTE DE SOUHAITS", "wishlist"),
            };
            setActive(items[0], (Label) items[0].getChildren().get(0), (Label) items[0].getChildren().get(1));
            nav.getChildren().addAll(items);
        } else if (session.isArtist()) {
            // Artist: My stuff
            HBox[] items = {
                    buildNavItem("⌂",  "ACCUEIL",           "home"),
                    buildNavItem("≡",  "MES PRODUITS",      "products"),
                    buildNavItem("◉",  "COMMANDES",         "orders"),
                    buildNavItem("★",  "AVIS CLIENTS",      "reviews"),
            };
            setActive(items[0], (Label) items[0].getChildren().get(0), (Label) items[0].getChildren().get(1));
            nav.getChildren().addAll(items);
        } else {
            // Admin: full control
            HBox[] items = {
                    buildNavItem("⌂",  "ACCUEIL",           "home"),
                    buildNavItem("≡",  "PRODUITS",          "products"),
                    buildNavItem("◉",  "COMMANDES",         "orders"),
                    buildNavItem("★",  "AVIS",              "reviews"),
                    buildNavItem("♥",  "LISTE DE SOUHAITS", "wishlist"),
            };
            setActive(items[0], (Label) items[0].getChildren().get(0), (Label) items[0].getChildren().get(1));
            nav.getChildren().addAll(items);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // User panel
        VBox userPanel = new VBox(4);
        userPanel.setPadding(new Insets(14, 16, 18, 16));
        userPanel.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 8; " +
                "-fx-border-color: #c0c0c0; -fx-border-radius: 8; -fx-border-width: 1;");
        VBox.setMargin(userPanel, new Insets(0, 14, 20, 14));

        Label userName = new Label(session.getName());
        userName.setStyle("-fx-text-fill: #c0c0c0; -fx-font-weight: bold; -fx-font-size: 13;");

        String roleText = session.isAdmin()  ? "Administrateur"
                        : session.isArtist() ? "Artiste"
                        : "Client";
        Label userRole = new Label(roleText);
        userRole.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");
        userPanel.getChildren().addAll(userName, userRole);

        // Logout
        Button logoutBtn = new Button("⎊  Déconnexion");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setPadding(new Insets(10, 16, 10, 16));
        logoutBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #666; " +
                "-fx-border-color: #2a2a2a; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 12;");
        logoutBtn.setOnMouseEntered(e -> logoutBtn.setStyle(
                "-fx-background-color: #2a2a2a; -fx-text-fill: #ff6b6b; " +
                        "-fx-border-color: #3a3a3a; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 12;"));
        logoutBtn.setOnMouseExited(e -> logoutBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #666; " +
                        "-fx-border-color: #2a2a2a; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 12;"));
        logoutBtn.setOnAction(e -> {
            SessionManager.getInstance().setRole(null);
            SessionManager.getInstance().setName(null);
            activeNavItem  = null;
            activeNavIcon  = null;
            activeNavLabel = null;
            showLoginScreen();
        });
        VBox.setMargin(logoutBtn, new Insets(0, 14, 14, 14));

        sidebar.getChildren().addAll(brand, sep, nav, spacer, userPanel, logoutBtn);
        return sidebar;
    }

    // ── Nav item ──────────────────────────────────────────────────
    private HBox buildNavItem(String icon, String label, String section) {
        Label iconLbl  = new Label(icon);
        Label labelLbl = new Label(label);
        iconLbl.setStyle("-fx-text-fill: #555; -fx-font-size: 14; -fx-min-width: 24;");
        labelLbl.setStyle("-fx-text-fill: #666; -fx-font-size: 11; -fx-font-weight: bold;");

        HBox item = new HBox(12, iconLbl, labelLbl);
        item.setPadding(new Insets(13, 20, 13, 20));
        item.setAlignment(Pos.CENTER_LEFT);
        item.setCursor(Cursor.HAND);

        item.setOnMouseEntered(e -> {
            if (item != activeNavItem) item.setStyle("-fx-background-color: #1a1a1a;");
        });
        item.setOnMouseExited(e -> {
            if (item != activeNavItem) item.setStyle("");
        });
        item.setOnMouseClicked(e -> {
            setActive(item, iconLbl, labelLbl);
            showSection(section);
        });
        return item;
    }

    // ── Activate nav item ─────────────────────────────────────────
    private void setActive(HBox item, Label iconLbl, Label labelLbl) {
        if (activeNavItem != null) {
            activeNavItem.setStyle("");
            activeNavIcon.setStyle("-fx-text-fill: #555; -fx-font-size: 14; -fx-min-width: 24;");
            activeNavLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11; -fx-font-weight: bold;");
        }
        item.setStyle("-fx-background-color: #1a1a1a; " +
                "-fx-border-color: transparent transparent transparent #c0c0c0; -fx-border-width: 3;");
        iconLbl.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 14; -fx-min-width: 24;");
        labelLbl.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 11; -fx-font-weight: bold;");
        activeNavItem  = item;
        activeNavIcon  = iconLbl;
        activeNavLabel = labelLbl;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
