package com.example.mythoriadesktop;

import com.example.mythoriadesktop.data.UserRepository;
import com.example.mythoriadesktop.model.User;
import com.example.mythoriadesktop.services.OtpService;
import com.example.mythoriadesktop.services.TwilioSmsService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    private final UserRepository userRepository = new UserRepository();
    private final OtpService otpService = new OtpService();
    private final TwilioSmsService smsService = new TwilioSmsService();
    private Stage stage;

    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;
        showLoginScene();
        stage.show();
    }

    private void showLoginScene() throws IOException {
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
        Parent root = loader.load();
        LoginController controller = loader.getController();
        controller.init(userRepository, this::showOtpScene);

        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(HelloApplication.class.getResource("style.css").toExternalForm());
        stage.setTitle("Mythoria - Login");
        stage.setScene(scene);
    }

    private void showOtpScene(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("OtpVerification.fxml"));
            Parent root = loader.load();
            OtpVerificationController controller = loader.getController();
            controller.init(user, otpService, smsService, this::showMainScene, this::handleLogout);

            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(HelloApplication.class.getResource("style.css").toExternalForm());
            stage.setTitle("Mythoria - OTP Verification");
            stage.setScene(scene);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load OTP verification view", ex);
        }
    }

    private void showMainScene(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("dashboard-view.fxml"));
            Parent root = loader.load();
            DashboardController controller = loader.getController();
            controller.init(user, this::handleLogout);

            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(HelloApplication.class.getResource("style.css").toExternalForm());
            stage.setTitle("Mythoria - Dashboard");
            stage.setScene(scene);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load main application view", ex);
        }
    }

    private void handleLogout() {
        try {
            showLoginScene();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to return to login view", ex);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
