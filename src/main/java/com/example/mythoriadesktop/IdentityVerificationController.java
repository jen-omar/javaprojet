package com.example.mythoriadesktop;

import com.example.mythoriadesktop.model.User;
import com.example.mythoriadesktop.services.VisionService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;

public class IdentityVerificationController {
    @FXML
    private ComboBox<String> documentTypeComboBox;

    @FXML
    private TextField selectedFileField;

    @FXML
    private Label verificationStatusLabel;

    @FXML
    private Label fullNameValueLabel;

    @FXML
    private Label birthDateValueLabel;

    @FXML
    private Label documentNumberValueLabel;

    @FXML
    private Label readableValueLabel;

    @FXML
    private TextArea extractedTextArea;

    @FXML
    private Label identityMessageLabel;

    private VisionService visionService;
    private Runnable onBack;
    private User currentUser;
    private Path selectedImagePath;

    @FXML
    private void initialize() {
        documentTypeComboBox.getItems().setAll("CIN", "PASSPORT");
        documentTypeComboBox.setValue("CIN");
        resetResult();
    }

    public void init(Runnable onBack) {
        this.onBack = onBack;
    }

    public void setUser(User user) {
        this.currentUser = user;
        resetResult();
    }

    @FXML
    private void onSelectImage() {
        Window window = selectedFileField == null || selectedFileField.getScene() == null
                ? null
                : selectedFileField.getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select CIN or Passport image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.bmp"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(window);
        if (selectedFile == null) {
            return;
        }

        selectedImagePath = selectedFile.toPath();
        selectedFileField.setText(selectedFile.getAbsolutePath());
        showMessage("Document image selected.", false);
    }

    @FXML
    private void onVerifyIdentity() {
        if (currentUser == null) {
            showMessage("No connected user was found.", true);
            return;
        }
        if (selectedImagePath == null) {
            showMessage("Please select a CIN or passport image first.", true);
            return;
        }

        try {
            VisionService.VerificationResult result = getVisionService().verifyIdentity(
                    currentUser,
                    documentTypeComboBox.getValue(),
                    selectedImagePath
            );
            applyResult(result);
            showMessage(result.statusMessage(), !result.verified());
        } catch (Exception ex) {
            String message = ex.getMessage();
            Throwable cause = ex.getCause();
            while ((message == null || message.isBlank()) && cause != null) {
                message = cause.getMessage();
                cause = cause.getCause();
            }
            showMessage(message == null || message.isBlank()
                    ? "Identity verification failed."
                    : message, true);
        }
    }

    @FXML
    private void onBack() {
        if (onBack != null) {
            onBack.run();
        }
    }

    private void applyResult(VisionService.VerificationResult result) {
        verificationStatusLabel.setText(Boolean.toString(result.verified()));
        fullNameValueLabel.setText(result.fullName().isBlank() ? "--" : result.fullName());
        birthDateValueLabel.setText(result.birthDate().isBlank() ? "--" : result.birthDate());
        documentNumberValueLabel.setText(result.documentNumber().isBlank() ? "--" : result.documentNumber());
        readableValueLabel.setText(result.extractedText().isBlank() ? "false" : "true");
        extractedTextArea.setText(result.extractedText());
    }

    private void resetResult() {
        if (verificationStatusLabel == null) {
            return;
        }
        verificationStatusLabel.setText("false");
        fullNameValueLabel.setText("--");
        birthDateValueLabel.setText("--");
        documentNumberValueLabel.setText("--");
        readableValueLabel.setText("false");
        if (extractedTextArea != null) {
            extractedTextArea.clear();
        }
    }

    private void showMessage(String message, boolean error) {
        identityMessageLabel.setText(message);
        identityMessageLabel.getStyleClass().removeAll("login-error", "login-success");
        if (!message.isBlank()) {
            identityMessageLabel.getStyleClass().add(error ? "login-error" : "login-success");
        }
    }

    private VisionService getVisionService() {
        if (visionService == null) {
            visionService = new VisionService();
        }
        return visionService;
    }
}
