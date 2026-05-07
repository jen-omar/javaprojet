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
    
    public Node getMarketplaceView(Runnable onMarketplaceLogout) {
        tn.esprit.Models.User user = tn.esprit.util.UserSession.getInstance().getUser();
        if (user != null) {
            String role = user.role();
            if (role == null || role.isBlank()) {
                role = "client";
            } else if (role.toLowerCase().contains("admin")) {
                role = "admin";
            } else if (role.toLowerCase().contains("artist") || role.toLowerCase().contains("artiste") || role.toLowerCase().contains("role_author") || role.toLowerCase().contains("author")) {
                role = "artist";
            } else {
                role = "client";
            }
            
            SessionManager.getInstance().setRole(role);
            String name = user.displayName();
            if (name == null || name.isBlank()) {
                name = user.username();
            }
            SessionManager.getInstance().setName(name);
            
            homeController     = new HomeController();
            productController  = new ProductController();
            orderController    = new OrderController();
            bidController      = new BidController();
            reviewController   = new ReviewController();
            wishlistController = new WishlistController();
            clientController   = new ClientController();

            BorderPane root = new BorderPane();
            root.setStyle("-fx-background-color: #1a1a1a;");
            root.setTop(buildNavbar(onMarketplaceLogout));

            contentArea = new StackPane();
            contentArea.setStyle("-fx-background-color: #1a1a1a;");
            root.setCenter(contentArea);

            showSection(SessionManager.getInstance().isClient() ? "catalogue" : "home");

            try {
                String css = getClass().getResource("/styles/dark-silver.css").toExternalForm();
                root.getStylesheets().add(css);
            } catch (Exception ignored) {}

            return root;
        } else {
            return new Label("Please log in from the main project first.");
        }
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
        root.setTop(buildNavbar(() -> {
            showLoginScreen();
        }));

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
        primaryStage.show();
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

    // ── Navbar — role-based ──────────────────────────────────────
    private HBox buildNavbar(Runnable onLogout) {
        SessionManager session = SessionManager.getInstance();
        HBox navbar = new HBox(0);
        navbar.setPrefHeight(60);
        navbar.setStyle("-fx-background-color: #111111;");
        navbar.setAlignment(Pos.CENTER_LEFT);

        // Brand
        HBox brand = new HBox(8);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(10, 20, 10, 20));
        
        javafx.scene.image.ImageView logoView = new javafx.scene.image.ImageView(
                new javafx.scene.image.Image(getClass().getResourceAsStream("/images/logo.jpg"))
        );
        logoView.setFitHeight(30);
        logoView.setPreserveRatio(true);

        Label brandSub = new Label("Art & Collection");
        brandSub.setStyle("-fx-text-fill: #444; -fx-font-size: 13; -fx-font-weight: bold;");
        brand.getChildren().addAll(logoView, brandSub);

        Region sep = new Region();
        sep.setPrefWidth(1);
        sep.setStyle("-fx-background-color: #222;");
        HBox.setMargin(sep, new Insets(10, 15, 10, 5));

        // Nav
        HBox nav = new HBox(0);
        nav.setAlignment(Pos.CENTER_LEFT);

        if (session.isClient()) {
            // Client: Catalogue + Avis + Liste de souhaits (sans Accueil)
            HBox[] items = {
                    buildNavItem("🛍", "CATALOGUE",         "catalogue"),
                    buildNavItem("★",  "AVIS",              "reviews"),
                    buildNavItem("♥",  "LISTE DE SOUHAITS", "wishlist"),
            };
            setActive(items[0], (Label) ((HBox)items[0].getChildren().get(0)).getChildren().get(0), (Label) ((HBox)items[0].getChildren().get(0)).getChildren().get(1));
            nav.getChildren().addAll(items);
        } else if (session.isArtist()) {
            // Artist: My stuff
            HBox[] items = {
                    buildNavItem("⌂",  "ACCUEIL",           "home"),
                    buildNavItem("≡",  "MES PRODUITS",      "products"),
                    buildNavItem("◉",  "COMMANDES",         "orders"),
                    buildNavItem("★",  "AVIS CLIENTS",      "reviews"),
            };
            setActive(items[0], (Label) ((HBox)items[0].getChildren().get(0)).getChildren().get(0), (Label) ((HBox)items[0].getChildren().get(0)).getChildren().get(1));
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
            setActive(items[0], (Label) ((HBox)items[0].getChildren().get(0)).getChildren().get(0), (Label) ((HBox)items[0].getChildren().get(0)).getChildren().get(1));
            nav.getChildren().addAll(items);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        navbar.getChildren().addAll(brand, sep, nav, spacer);
        return navbar;
    }

    // ── Nav item ──────────────────────────────────────────────────
    private HBox buildNavItem(String icon, String label, String section) {
        Label iconLbl  = new Label(icon);
        Label labelLbl = new Label(label);
        iconLbl.setStyle("-fx-text-fill: #555; -fx-font-size: 14;");
        labelLbl.setStyle("-fx-text-fill: #666; -fx-font-size: 11; -fx-font-weight: bold;");

        HBox topPart = new HBox(6, iconLbl, labelLbl);
        topPart.setAlignment(Pos.CENTER);
        
        HBox item = new HBox(topPart);
        item.setPadding(new Insets(0, 16, 0, 16));
        item.setAlignment(Pos.CENTER);
        item.setPrefHeight(60);
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
            activeNavIcon.setStyle("-fx-text-fill: #555; -fx-font-size: 14;");
            activeNavLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11; -fx-font-weight: bold;");
        }
        item.setStyle("-fx-background-color: #1a1a1a; " +
                "-fx-border-color: transparent transparent #c0c0c0 transparent; -fx-border-width: 0 0 3 0;");
        iconLbl.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 14;");
        labelLbl.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 11; -fx-font-weight: bold;");
        activeNavItem  = item;
        activeNavIcon  = iconLbl;
        activeNavLabel = labelLbl;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
