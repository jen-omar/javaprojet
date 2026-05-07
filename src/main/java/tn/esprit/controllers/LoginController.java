package tn.esprit.controllers;

import tn.esprit.data.UserRepository;
import tn.esprit.Models.User;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class LoginController {
    private static final PseudoClass INVALID_PSEUDO_CLASS = PseudoClass.getPseudoClass("invalid");

    @FXML
    private VBox loginPane;

    @FXML
    private VBox signupPane;

    @FXML
    private Label authTitleLabel;

    @FXML
    private Label authSubtitleLabel;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField signupUsernameField;

    @FXML
    private TextField signupEmailField;

    @FXML
    private TextField signupPhoneField;

    @FXML
    private TextField signupFirstNameField;

    @FXML
    private TextField signupLastNameField;

    @FXML
    private PasswordField signupPasswordField;

    @FXML
    private PasswordField signupConfirmPasswordField;

    @FXML
    private Label feedbackLabel;

    private UserRepository userRepository;
    private Consumer<User> onLoginSuccess;
    private boolean signupMode;

    @FXML
    private void initialize() {
        configureEmailField(signupEmailField);
        configurePhoneField(signupPhoneField);
    }

    public void init(UserRepository userRepository, Consumer<User> onLoginSuccess) {
        this.userRepository = userRepository;
        this.onLoginSuccess = onLoginSuccess;
        showLoginMode();
    }

    @FXML
    private void onLogin() {
        if (userRepository == null || onLoginSuccess == null) {
            setFeedback("Le module de connexion n'est pas initialise.", true);
            return;
        }

        String username = Optional.ofNullable(usernameField.getText()).orElse("").trim();
        String password = Optional.ofNullable(passwordField.getText()).orElse("");

        if (username.isBlank() || password.isBlank()) {
            setFeedback("Renseigne ton identifiant et ton mot de passe.", true);
            return;
        }

        try {
            ValidationUtils.requireUsername(username);
        } catch (IllegalArgumentException ignored) {
            try {
                ValidationUtils.requireEmail(username);
            } catch (IllegalArgumentException ex) {
                setFeedback("Identifiant invalide. Utilise un username ou un email valide.", true);
                return;
            }
        }

        Optional<User> user = userRepository.authenticate(username, password);
        if (user.isPresent()) {
            setFeedback("", false);
            onLoginSuccess.accept(user.get());
            return;
        }

        setFeedback("Identifiants invalides.", true);
        passwordField.clear();
        passwordField.requestFocus();
    }

    @FXML
    private void onSignup() {
        if (userRepository == null || onLoginSuccess == null) {
            setFeedback("Le module d'inscription n'est pas initialise.", true);
            return;
        }

        String username = read(signupUsernameField);
        String email = read(signupEmailField);
        String phone = read(signupPhoneField);
        String firstName = read(signupFirstNameField);
        String lastName = read(signupLastNameField);
        String password = Optional.ofNullable(signupPasswordField.getText()).orElse("");
        String confirmPassword = Optional.ofNullable(signupConfirmPasswordField.getText()).orElse("");

        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            setFeedback("Username, email et mot de passe sont obligatoires.", true);
            return;
        }

        if (!password.equals(confirmPassword)) {
            setFeedback("La confirmation du mot de passe ne correspond pas.", true);
            signupConfirmPasswordField.clear();
            signupConfirmPasswordField.requestFocus();
            return;
        }

        try {
            ValidationUtils.requireUsername(username);
            email = ValidationUtils.requireEmail(email);
            phone = ValidationUtils.optionalPhone(phone);
            signupEmailField.setText(email);
            signupPhoneField.setText(phone);
            ValidationUtils.optionalName(firstName, "Prenom");
            ValidationUtils.optionalName(lastName, "Nom");
            ValidationUtils.requireStrongPassword(password);
            User createdUser = userRepository.registerUser(username, email, password, firstName, lastName, phone);
            setFeedback("Compte cree avec succes. Connexion en cours...", false);
            onLoginSuccess.accept(createdUser);
        } catch (IllegalArgumentException ex) {
            setFeedback(ex.getMessage(), true);
        } catch (Exception ex) {
            setFeedback("Impossible de creer le compte.", true);
        }
    }

    @FXML
    private void onShowLogin() {
        showLoginMode();
    }

    @FXML
    private void onShowSignup() {
        showSignupMode();
    }

    private void showLoginMode() {
        signupMode = false;
        authTitleLabel.setText("Connexion a Mythoria");
        authSubtitleLabel.setText("Entre avec ton username ou ton email pour acceder a ton profil, ton wallet et tes outils.");
        loginPane.setVisible(true);
        loginPane.setManaged(true);
        signupPane.setVisible(false);
        signupPane.setManaged(false);
        setFeedback("", false);
    }

    private void showSignupMode() {
        signupMode = true;
        authTitleLabel.setText("Creer un compte Mythoria");
        authSubtitleLabel.setText("Inscris-toi pour acceder au profil, au wallet et aux interfaces de la plateforme.");
        loginPane.setVisible(false);
        loginPane.setManaged(false);
        signupPane.setVisible(true);
        signupPane.setManaged(true);
        setFeedback("", false);
        updateEmailFieldState(signupEmailField);
        updatePhoneFieldState(signupPhoneField);
    }

    private void configureEmailField(TextField field) {
        if (field == null) {
            return;
        }
        field.textProperty().addListener((obs, oldValue, newValue) -> updateEmailFieldState(field));
        field.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                field.setText(ValidationUtils.normalizeEmail(field.getText()));
            }
            updateEmailFieldState(field);
        });
        updateEmailFieldState(field);
    }

    private void updateEmailFieldState(TextField field) {
        String value = Optional.ofNullable(field.getText()).orElse("");
        boolean invalid = !value.isBlank() && !ValidationUtils.isValidEmailFormat(value);
        field.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, invalid);
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
        field.textProperty().addListener((obs, oldValue, newValue) -> updatePhoneFieldState(field));
        field.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                field.setText(ValidationUtils.normalizePhone(field.getText()));
            }
            updatePhoneFieldState(field);
        });
        updatePhoneFieldState(field);
    }

    private void updatePhoneFieldState(TextField field) {
        String value = Optional.ofNullable(field.getText()).orElse("");
        boolean invalid = !ValidationUtils.isValidPhoneFormat(value);
        field.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, invalid);
    }

    private void setFeedback(String message, boolean error) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("login-error", "login-success");
        if (!message.isBlank()) {
            feedbackLabel.getStyleClass().add(error ? "login-error" : "login-success");
        }
    }

    private String read(TextField field) {
        return Optional.ofNullable(field.getText()).orElse("").trim();
    }
}
