package com.example.mythoriadesktop;

import com.example.mythoriadesktop.data.UserRepository;
import com.example.mythoriadesktop.data.WalletRepository;
import com.example.mythoriadesktop.model.User;
import com.example.mythoriadesktop.services.EmailNotificationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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
    private EmailNotificationService emailNotificationService;
    private Runnable onLogout;
    private Node placeholderView;
    private Label placeholderTitle;
    private Label placeholderSubtitle;
    private Label placeholderNote;
    private Button activeNavButton;
    private User currentUser;

    @FXML
    public void initialize() {
        loadProfile();
        loadAdmin();
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
                showPlaceholder(
                        "Kinship",
                        "Cet espace pourra accueillir les relations et groupes.",
                        "Interface vide pour le moment, avec le meme design que le dashboard."
                );
            }
            case "BAZAAR" -> {
                setActiveNav(navBazaar);
                showPlaceholder(
                        "Bazaar",
                        "Cet espace pourra accueillir le marche et les echanges.",
                        "Interface vide pour le moment, avec le meme design que le dashboard."
                );
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

    private void showHome() {
        profileHost.setVisible(false);
        profileHost.setManaged(false);
        adminHost.setVisible(false);
        adminHost.setManaged(false);
        showPlaceholder(
                "Mythoria Dashboard",
                "Accede rapidement a ton profil, ton wallet et aux outils admin.",
                "Navigation disponible: Home, Worlds, Kinship, Bazaar, Profile et Admin selon le role."
        );
    }

    private void showProfile() {
        hidePlaceholder();
        adminHost.setVisible(false);
        adminHost.setManaged(false);
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
        profileHost.setVisible(false);
        profileHost.setManaged(false);
        adminHost.setVisible(true);
        adminHost.setManaged(true);
        if (adminController != null) {
            adminController.setCurrentUser(currentUser);
        }
    }

    private void showPlaceholder(String title, String subtitle, String note) {
        profileHost.setVisible(false);
        profileHost.setManaged(false);
        adminHost.setVisible(false);
        adminHost.setManaged(false);

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
