package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.Models.User;
import tn.esprit.services.UserDAO;
import tn.esprit.util.UserSession;

import java.io.IOException;

public class LoginController {

    @FXML private VBox loginForm;
    @FXML private VBox registerForm;
    
    @FXML private TextField loginEmail;
    @FXML private PasswordField loginPassword;
    
    @FXML private TextField regEmail;
    @FXML private TextField regUsername;
    @FXML private PasswordField regPassword;
    @FXML private ComboBox<String> regRoleSelector;
    
    @FXML private Label errorLabel;

    private final UserDAO userDAO = new UserDAO();
    private boolean isLoginView = true;

    @FXML
    public void initialize() {
        regRoleSelector.setItems(FXCollections.observableArrayList("Author (Selling)", "Client (Buying)", "Admin (Overseer)"));
    }

    @FXML
    public void switchForm() {
        isLoginView = !isLoginView;
        loginForm.setVisible(isLoginView);
        registerForm.setVisible(!isLoginView);
        errorLabel.setVisible(false);
    }

    @FXML
    public void onLogin() {
        String identifier = loginEmail.getText();
        String pass = loginPassword.getText();
        
        if (identifier.isBlank() || pass.isBlank()) {
            showError("Fields cannot be empty.");
            return;
        }

        User user = userDAO.login(identifier, pass);
        if (user != null) {
            UserSession.getInstance().setUser(user);
            loadMainDashboard();
        } else {
            showError("Invalid email/username or password.");
        }
    }

    @FXML
    public void onRegister() {
        String email = regEmail.getText();
        String username = regUsername.getText();
        String pass = regPassword.getText();
        String roleSelection = regRoleSelector.getValue();

        if (email.isBlank() || username.isBlank() || pass.isBlank() || roleSelection == null) {
            showError("All fields must be filled.");
            return;
        }

        if (userDAO.checkEmailExists(email) != null) {
            showError("Email is already bound to another soul.");
            return;
        }

        String actualRole;
        if (roleSelection.contains("Admin")) {
            actualRole = "[\"ROLE_ADMIN\"]";
        } else if (roleSelection.contains("Author")) {
            actualRole = "[\"ROLE_AUTHOR\"]";
        } else {
            actualRole = "[\"ROLE_CLIENT\"]";
        }
        User newUser = new User(email, username, pass, false, actualRole);
        userDAO.add(newUser);
        
        // Auto-login after registration
        User savedUser = userDAO.login(email, pass);
        if (savedUser != null) {
            UserSession.getInstance().setUser(savedUser);
            loadMainDashboard();
        } else {
            showError("Sign Up failed. Please check your database connection or schema.");
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void loadMainDashboard() {
        try {
            Stage stage = (Stage) loginEmail.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
            scene.getStylesheets().add(HelloApplication.class.getResource("style.css").toExternalForm());
            stage.setTitle("Mythoria - The Grand Archives");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load dashboard: " + e.getMessage());
        }
    }
}
