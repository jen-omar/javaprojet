package com.example.mythoriadesktop;

import com.example.mythoriadesktop.data.UserRepository;
import com.example.mythoriadesktop.data.WalletRepository;
import com.example.mythoriadesktop.model.User;
import javafx.css.PseudoClass;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class ProfileController {
    private static final PseudoClass INVALID_PSEUDO_CLASS = PseudoClass.getPseudoClass("invalid");

    @FXML
    private Label profileHeadline;

    @FXML
    private Label profileMeta;

    @FXML
    private Label profileRank;

    @FXML
    private Label profilePoints;

    @FXML
    private Label profileSource;

    @FXML
    private Label profileMessage;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField phoneField;

    @FXML
    private VBox profileContent;

    @FXML
    private StackPane walletHost;

    @FXML
    private StackPane identityVerificationHost;

    private UserRepository userRepository;
    private final WalletRepository walletRepository = new WalletRepository();
    private Consumer<User> onUserUpdated;
    private User currentUser;
    private WalletController walletController;
    private IdentityVerificationController identityVerificationController;

    @FXML
    private void initialize() {
        configureEmailField(emailField);
        configurePhoneField(phoneField);
    }

    public void init(UserRepository userRepository, Consumer<User> onUserUpdated) {
        this.userRepository = userRepository;
        this.onUserUpdated = onUserUpdated;
    }

    public void setUser(User user) {
        currentUser = user;
        renderUser();
    }

    @FXML
    private void onSaveProfile() {
        requireInitialization();
        if (currentUser == null) {
            showMessage("Aucun profil charge.", true);
            return;
        }

        try {
            String email = ValidationUtils.requireEmail(emailField.getText());
            String phone = ValidationUtils.optionalPhone(phoneField.getText());
            emailField.setText(email);
            phoneField.setText(phone);
            ValidationUtils.optionalName(firstNameField.getText(), "Prenom");
            ValidationUtils.optionalName(lastNameField.getText(), "Nom");
            User updated = userRepository.updateProfile(
                    currentUser,
                    email,
                    firstNameField.getText(),
                    lastNameField.getText(),
                    phone
            );
            currentUser = updated;
            renderUser();
            showMessage("Profil enregistre.", false);
            if (onUserUpdated != null) {
                onUserUpdated.accept(updated);
            }
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage(), true);
        } catch (Exception ex) {
            showMessage("Impossible d'enregistrer le profil.", true);
        }
    }

    @FXML
    private void onReloadProfile() {
        requireInitialization();
        if (currentUser == null) {
            showMessage("Aucun profil charge.", true);
            return;
        }

        Optional<User> reloaded = userRepository.reloadUser(currentUser);
        if (reloaded.isEmpty()) {
            showMessage("Impossible de recharger le profil.", true);
            return;
        }

        currentUser = reloaded.get();
        renderUser();
        showMessage("Profil recharge.", false);
        if (onUserUpdated != null) {
            onUserUpdated.accept(currentUser);
        }
    }

    @FXML
    private void onOpenWallet() {
        requireInitialization();
        if (walletController == null) {
            try {
                loadWalletView();
            } catch (Exception ex) {
                String detail = ex.getMessage() == null || ex.getMessage().isBlank() ? ex.getClass().getSimpleName() : ex.getMessage();
                showMessage("Impossible d'ouvrir le wallet: " + detail, true);
                return;
            }
        }

        walletController.setUser(currentUser);
        profileContent.setVisible(false);
        profileContent.setManaged(false);
        identityVerificationHost.setVisible(false);
        identityVerificationHost.setManaged(false);
        walletHost.setVisible(true);
        walletHost.setManaged(true);
        showMessage("", false);
    }

    @FXML
    private void onOpenIdentityVerification() {
        requireInitialization();
        if (identityVerificationController == null) {
            try {
                loadIdentityVerificationView();
            } catch (Exception ex) {
                String detail = ex.getMessage() == null || ex.getMessage().isBlank()
                        ? ex.getClass().getSimpleName()
                        : ex.getMessage();
                showMessage("Impossible d'ouvrir la verification d'identite: " + detail, true);
                return;
            }
        }

        identityVerificationController.setUser(currentUser);
        profileContent.setVisible(false);
        profileContent.setManaged(false);
        walletHost.setVisible(false);
        walletHost.setManaged(false);
        identityVerificationHost.setVisible(true);
        identityVerificationHost.setManaged(true);
        showMessage("", false);
    }

    private void onCloseWallet() {
        walletHost.setVisible(false);
        walletHost.setManaged(false);
        profileContent.setVisible(true);
        profileContent.setManaged(true);
    }

    private void onCloseIdentityVerification() {
        identityVerificationHost.setVisible(false);
        identityVerificationHost.setManaged(false);
        profileContent.setVisible(true);
        profileContent.setManaged(true);
    }

    private void renderUser() {
        if (usernameField == null) {
            return;
        }

        if (currentUser == null) {
            profileHeadline.setText("Profile");
            profileMeta.setText("Aucun utilisateur connecte.");
            profileRank.setText("Rank: --");
            profilePoints.setText("-- PC");
            profileSource.setText("Source: --");
            usernameField.setText("");
            emailField.setText("");
            firstNameField.setText("");
            lastNameField.setText("");
            phoneField.setText("");
            if (walletController != null) {
                walletController.setUser(null);
            }
            if (identityVerificationController != null) {
                identityVerificationController.setUser(null);
            }
            return;
        }

        String fullName = (currentUser.firstName() + " " + currentUser.lastName()).trim();
        if (fullName.isBlank()) {
            fullName = currentUser.displayName();
        }

        profileHeadline.setText(fullName.isBlank() ? currentUser.username() : fullName);
        profileMeta.setText("@" + currentUser.username());
        profileRank.setText("Rank: " + currentUser.rank());
        profilePoints.setText(currentUser.points() + " PC");
        profileSource.setText(currentUser.databaseBacked() ? "Source: MySQL" : "Source: Local");

        usernameField.setText(currentUser.username());
        emailField.setText(currentUser.email());
        firstNameField.setText(currentUser.firstName());
        lastNameField.setText(currentUser.lastName());
        phoneField.setText(currentUser.phoneNumber());
        updateEmailFieldState();
        updatePhoneFieldState();
        if (walletController != null) {
            walletController.setUser(currentUser);
        }
        if (identityVerificationController != null) {
            identityVerificationController.setUser(currentUser);
        }
    }

    private void configureEmailField(TextField field) {
        if (field == null) {
            return;
        }
        field.textProperty().addListener((obs, oldValue, newValue) -> updateEmailFieldState());
        field.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                field.setText(ValidationUtils.normalizeEmail(field.getText()));
            }
            updateEmailFieldState();
        });
        updateEmailFieldState();
    }

    private void configurePhoneField(TextField field) {
        if (field == null) {
            return;
        }

        UnaryOperator<TextFormatter.Change> filter = change -> {
            String nextText = change.getControlNewText();
            if (ValidationUtils.isValidPhoneInput(nextText)) {
                return change;
            }
            return null;
        };
        field.setTextFormatter(new TextFormatter<>(filter));
        field.textProperty().addListener((obs, oldValue, newValue) -> updatePhoneFieldState());
        field.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                field.setText(ValidationUtils.normalizePhone(field.getText()));
            }
            updatePhoneFieldState();
        });
        updatePhoneFieldState();
    }

    private void updateEmailFieldState() {
        String value = emailField == null ? "" : Optional.ofNullable(emailField.getText()).orElse("");
        boolean invalid = !value.isBlank() && !ValidationUtils.isValidEmailFormat(value);
        if (emailField != null) {
            emailField.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, invalid);
        }
    }

    private void updatePhoneFieldState() {
        String value = phoneField == null ? "" : Optional.ofNullable(phoneField.getText()).orElse("");
        boolean invalid = !ValidationUtils.isValidPhoneFormat(value);
        if (phoneField != null) {
            phoneField.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, invalid);
        }
    }

    private void showMessage(String message, boolean error) {
        profileMessage.setText(message);
        profileMessage.getStyleClass().removeAll("login-error", "login-success");
        if (!message.isBlank()) {
            profileMessage.getStyleClass().add(error ? "login-error" : "login-success");
        }
    }

    private void requireInitialization() {
        if (userRepository == null) {
            throw new IllegalStateException("ProfileController not initialized");
        }
    }

    private void loadWalletView() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("wallet-view.fxml"));
            Node walletView = loader.load();
            walletController = loader.getController();
            walletController.init(walletRepository, this::onCloseWallet);
            walletHost.getChildren().setAll(walletView);
            walletHost.setVisible(false);
            walletHost.setManaged(false);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load wallet-view.fxml", ex);
        }
    }

    private void loadIdentityVerificationView() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("IdentityVerification.fxml"));
            Node identityView = loader.load();
            identityVerificationController = loader.getController();
            identityVerificationController.init(this::onCloseIdentityVerification);
            identityVerificationHost.getChildren().setAll(identityView);
            identityVerificationHost.setVisible(false);
            identityVerificationHost.setManaged(false);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load IdentityVerification.fxml", ex);
        }
    }
}
