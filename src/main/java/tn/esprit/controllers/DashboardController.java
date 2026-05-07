package tn.esprit.controllers;

import tn.esprit.data.UserRepository;
import tn.esprit.data.WalletRepository;
import tn.esprit.Models.User;
import tn.esprit.controllers.services.EmailNotificationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.io.IOException;

public class DashboardController {
    private static final String NAV_ACTIVE_STYLE = "nav-active";

    @FXML
    private Label rankLabel;

    @FXML
    private Label pcLabel;

    @FXML
    private StackPane contentStack;

    @FXML
    private StackPane profileHost;

    @FXML
    private StackPane adminHost;

    @FXML
    private StackPane kinshipHost;

    @FXML
    private Button navHome;

    @FXML
    private Button navProfile;

    @FXML
    private Button navWorlds;

    @FXML
    private Button navKinship;

    @FXML
    private Button navBazaar;

    @FXML
    private Button navAdmin;

    @FXML
    private Button logoutButton;

    private final UserRepository userRepository = new UserRepository();
    private final WalletRepository walletRepository = new WalletRepository();

    private ProfileController profileController;
    private AdminController adminController;
    private KinshipController kinshipController;
    private EmailNotificationService emailNotificationService;
    private Runnable onLogout;
    private Node placeholderView;
    private Label placeholderTitle;
    private Label placeholderSubtitle;
    private Label placeholderNote;
    private Button activeNavButton;
    private User currentUser;
    private Node marketplaceView;
    private Node homeView;

    @FXML
    public void initialize() {
        loadProfile();
        loadAdmin();
        loadKinship();
        showHome();
    }

    public void init(User user, Runnable onLogout, EmailNotificationService emailNotificationService) {
        currentUser = user;
        this.onLogout = onLogout;
        this.emailNotificationService = emailNotificationService;
        if (adminController != null) {
            adminController.setEmailNotificationService(emailNotificationService);
        }
        applyCurrentUser();
    }

    @FXML
    private void onNavigate(ActionEvent event) {
        Object source = event.getSource();
        if (!(source instanceof Button button)) {
            return;
        }

        String route = button.getUserData() == null ? "" : button.getUserData().toString();
        switch (route) {
            case "HOME" -> {
                setActiveNav(navHome);
                showHome();
            }
            case "PROFILE" -> {
                setActiveNav(navProfile);
                showProfile();
            }
            case "WORLDS" -> {
                setActiveNav(navWorlds);
                showPlaceholder(
                        "Worlds",
                        "Cet espace est pret pour tes futurs mondes.",
                        "Interface vide pour le moment, avec le meme design que le dashboard."
                );
            }
            case "KINSHIP" -> {
                setActiveNav(navKinship);
                showKinship();
            }
            case "BAZAAR" -> {
                setActiveNav(navBazaar);
                showMarketplace();
            }
            case "ADMIN" -> {
                setActiveNav(navAdmin);
                showAdmin();
            }
            default -> {
                setActiveNav(navHome);
                showHome();
            }
        }
    }

    @FXML
    private void onLogout() {
        if (onLogout != null) {
            onLogout.run();
        }
    }

    private void loadProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("profile-view.fxml"));
            Node profileView = loader.load();
            profileController = loader.getController();
            profileController.init(userRepository, this::handleUserUpdated);
            profileHost.getChildren().setAll(profileView);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load profile-view.fxml", ex);
        }
    }

    private void loadAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("admin-view.fxml"));
            Node adminView = loader.load();
            adminController = loader.getController();
            adminController.init(userRepository, walletRepository);
            adminController.setEmailNotificationService(emailNotificationService);
            adminHost.getChildren().setAll(adminView);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load admin-view.fxml", ex);
        }
    }

    private void loadKinship() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("kinship-view.fxml"));
            Node kinshipView = loader.load();
            kinshipController = loader.getController();
            kinshipHost.getChildren().setAll(kinshipView);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load kinship-view.fxml", ex);
        }
    }

    private void showHome() {
        hidePlaceholder();
        if (homeView != null) {
            homeView.setVisible(false);
            homeView.setManaged(false);
        }
        profileHost.setVisible(false);
        profileHost.setManaged(false);
        adminHost.setVisible(false);
        adminHost.setManaged(false);
        if (kinshipHost != null) {
            kinshipHost.setVisible(false);
            kinshipHost.setManaged(false);
        }
        if (marketplaceView != null) {
            marketplaceView.setVisible(false);
            marketplaceView.setManaged(false);
        }

        if (homeView == null) {
            javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(20);
            box.setPadding(new javafx.geometry.Insets(40));
            box.setAlignment(Pos.TOP_LEFT);

            Label title = new Label("Bienvenue sur Mythoria Dashboard");
            title.setStyle("-fx-text-fill: white; -fx-font-size: 32; -fx-font-weight: bold;");

            Label desc = new Label("Votre hub central pour gérer votre collection d'art, " +
                    "explorer de nouveaux mondes et interagir avec la communauté.\n\n" +
                    "✓ Accédez à vos commandes et enchères récentes.\n" +
                    "✓ Découvrez les œuvres d'art du Bazaar.\n" +
                    "✓ Discutez avec notre Assistant IA.");
            desc.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 16;");
            desc.setWrapText(true);

            try {
                // Use File to get absolute URI which works better than getResource for Media
                java.io.File file = new java.io.File("src/main/resources/videos/animation.mp4");
                Media media = new Media(file.toURI().toString());
                MediaPlayer mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer.setAutoPlay(true);
                mediaPlayer.setOnError(() -> System.err.println("Media error: " + mediaPlayer.getError()));
                mediaPlayer.play(); // Explicit play to fix black screen
                
                MediaView mediaView = new MediaView(mediaPlayer);
                mediaView.setFitWidth(650);
                mediaView.setPreserveRatio(true);
                
                javafx.scene.layout.VBox videoContainer = new javafx.scene.layout.VBox(mediaView);
                videoContainer.setAlignment(Pos.CENTER);
                videoContainer.setPadding(new javafx.geometry.Insets(10));
                videoContainer.setStyle("-fx-border-color: #c9a84c; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-color: #1a1a1a; -fx-background-radius: 10;");
                videoContainer.setMaxWidth(670);
                
                box.getChildren().addAll(title, desc, videoContainer);
            } catch (Exception e) {
                System.err.println("Could not load home video: " + e.getMessage());
                box.getChildren().addAll(title, desc);
            }

            homeView = box;
            contentStack.getChildren().add(homeView);
        }
        
        homeView.setVisible(true);
        homeView.setManaged(true);
    }

    private void showProfile() {
        if (homeView != null) {
            homeView.setVisible(false);
            homeView.setManaged(false);
        }
        hidePlaceholder();
        adminHost.setVisible(false);
        adminHost.setManaged(false);
        if (kinshipHost != null) {
            kinshipHost.setVisible(false);
            kinshipHost.setManaged(false);
        }
        if (marketplaceView != null) {
            marketplaceView.setVisible(false);
            marketplaceView.setManaged(false);
        }
        profileHost.setVisible(true);
        profileHost.setManaged(true);
        if (profileController != null) {
            profileController.setUser(currentUser);
        }
    }

    private void showAdmin() {
        if (currentUser == null || !currentUser.isAdmin()) {
            setActiveNav(navHome);
            showHome();
            return;
        }

        hidePlaceholder();
        if (homeView != null) {
            homeView.setVisible(false);
            homeView.setManaged(false);
        }
        profileHost.setVisible(false);
        profileHost.setManaged(false);
        adminHost.setVisible(true);
        adminHost.setManaged(true);
        if (kinshipHost != null) {
            kinshipHost.setVisible(false);
            kinshipHost.setManaged(false);
        }
        if (marketplaceView != null) {
            marketplaceView.setVisible(false);
            marketplaceView.setManaged(false);
        }
        if (adminController != null) {
            adminController.setCurrentUser(currentUser);
        }
    }

    private void showKinship() {
        if (homeView != null) {
            homeView.setVisible(false);
            homeView.setManaged(false);
        }
        hidePlaceholder();
        profileHost.setVisible(false);
        profileHost.setManaged(false);
        adminHost.setVisible(false);
        adminHost.setManaged(false);
        if (kinshipHost != null) {
            kinshipHost.setVisible(true);
            kinshipHost.setManaged(true);
        }
        if (marketplaceView != null) {
            marketplaceView.setVisible(false);
            marketplaceView.setManaged(false);
        }
    }

    private void showPlaceholder(String title, String subtitle, String note) {
        if (homeView != null) {
            homeView.setVisible(false);
            homeView.setManaged(false);
        }
        profileHost.setVisible(false);
        profileHost.setManaged(false);
        adminHost.setVisible(false);
        adminHost.setManaged(false);
        if (kinshipHost != null) {
            kinshipHost.setVisible(false);
            kinshipHost.setManaged(false);
        }
        if (marketplaceView != null) {
            marketplaceView.setVisible(false);
            marketplaceView.setManaged(false);
        }
        if (homeView != null) {
            homeView.setVisible(false);
            homeView.setManaged(false);
        }

        if (placeholderView == null) {
            VBox box = new VBox(12);
            box.getStyleClass().add("form-panel");
            box.setMaxWidth(780);
            box.setAlignment(Pos.CENTER_LEFT);

            placeholderTitle = new Label();
            placeholderTitle.getStyleClass().add("profile-title");

            placeholderSubtitle = new Label();
            placeholderSubtitle.getStyleClass().add("profile-subtitle");
            placeholderSubtitle.setWrapText(true);

            placeholderNote = new Label();
            placeholderNote.getStyleClass().add("world-desc");
            placeholderNote.setWrapText(true);

            box.getChildren().addAll(placeholderTitle, placeholderSubtitle, placeholderNote);
            placeholderView = box;
            contentStack.getChildren().add(0, placeholderView);
        }

        placeholderTitle.setText(title);
        placeholderSubtitle.setText(subtitle);
        placeholderNote.setText(note);
        placeholderView.setVisible(true);
        placeholderView.setManaged(true);
    }

    private void hidePlaceholder() {
        if (placeholderView != null) {
            placeholderView.setVisible(false);
            placeholderView.setManaged(false);
        }
    }

    private void showMarketplace() {
        hidePlaceholder();
        profileHost.setVisible(false);
        profileHost.setManaged(false);
        adminHost.setVisible(false);
        adminHost.setManaged(false);
        if (kinshipHost != null) {
            kinshipHost.setVisible(false);
            kinshipHost.setManaged(false);
        }
        if (homeView != null) {
            homeView.setVisible(false);
            homeView.setManaged(false);
        }
        
        if (marketplaceView == null) {
            com.marketplace.MainApp marketplaceApp = new com.marketplace.MainApp();
            marketplaceView = marketplaceApp.getMarketplaceView(() -> {
                setActiveNav(navHome);
                showHome();
            });
            contentStack.getChildren().add(marketplaceView);
        }
        marketplaceView.setVisible(true);
        marketplaceView.setManaged(true);
    }

    private void setActiveNav(Button button) {
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove(NAV_ACTIVE_STYLE);
        }
        activeNavButton = button;
        if (activeNavButton != null && !activeNavButton.getStyleClass().contains(NAV_ACTIVE_STYLE)) {
            activeNavButton.getStyleClass().add(NAV_ACTIVE_STYLE);
        }
    }

    private void applyCurrentUser() {
        if (rankLabel == null || pcLabel == null) {
            return;
        }

        if (currentUser == null) {
            rankLabel.setText("Rank: Journeyman");
            pcLabel.setText("100 PC");
            navAdmin.setVisible(false);
            navAdmin.setManaged(false);
            return;
        }

        rankLabel.setText("Rank: " + currentUser.rank());
        pcLabel.setText(currentUser.points() + " PC");
        navAdmin.setVisible(currentUser.isAdmin());
        navAdmin.setManaged(currentUser.isAdmin());

        if (profileController != null) {
            profileController.setUser(currentUser);
        }
        if (adminController != null) {
            adminController.setCurrentUser(currentUser);
        }
    }

    private void handleUserUpdated(User updatedUser) {
        currentUser = updatedUser;
        applyCurrentUser();
    }
}
