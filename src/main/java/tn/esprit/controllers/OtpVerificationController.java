package tn.esprit.controllers;

import tn.esprit.Models.User;
import tn.esprit.controllers.services.OtpService;
import tn.esprit.controllers.services.TwilioSmsService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.Optional;
import java.util.function.Consumer;

public class OtpVerificationController {
    @FXML
    private Label instructionLabel;

    @FXML
    private TextField otpField;

    @FXML
    private Button verifyButton;

    @FXML
    private Button resendButton;

    @FXML
    private Label feedbackLabel;

    private User user;
    private OtpService otpService;
    private TwilioSmsService smsService;
    private Consumer<User> onOtpVerified;
    private Runnable onCancel;

    @FXML
    private void initialize() {
        otpField.setTextFormatter(new TextFormatter<>(change -> {
            String nextText = change.getControlNewText();
            return nextText.matches("\\d{0,6}") ? change : null;
        }));
    }

    public void init(User user, OtpService otpService, TwilioSmsService smsService, Consumer<User> onOtpVerified, Runnable onCancel) {
        this.user = user;
        this.otpService = otpService;
        this.smsService = smsService;
        this.onOtpVerified = onOtpVerified;
        this.onCancel = onCancel;
        instructionLabel.setText("Code envoye au " + maskPhone(user.phoneNumber()) + ". Il expire dans "
                + otpService.expirationMinutes() + " minutes.");
        sendOtp();
    }

    @FXML
    private void onVerifyOtp() {
        String code = Optional.ofNullable(otpField.getText()).orElse("").trim();
        if (code.length() != 6) {
            setFeedback("Invalid or expired OTP", true);
            return;
        }

        try {
            if (otpService.verifyOtp(user, code)) {
                setFeedback("", false);
                onOtpVerified.accept(user);
                return;
            }
            setFeedback("Invalid or expired OTP", true);
            otpField.clear();
            otpField.requestFocus();
        } catch (Exception ex) {
            setFeedback("Invalid or expired OTP", true);
        }
    }

    @FXML
    private void onResendOtp() {
        sendOtp();
    }

    @FXML
    private void onCancel() {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    private void sendOtp() {
        setControlsDisabled(true);
        setFeedback("Envoi du code OTP...", false);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                String otpCode = otpService.generateAndSaveOtp(user);
                smsService.sendOtp(user.phoneNumber(), otpCode);
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            setControlsDisabled(false);
            setFeedback("Code OTP envoye par SMS.", false);
            Platform.runLater(otpField::requestFocus);
        });
        task.setOnFailed(event -> {
            setControlsDisabled(false);
            Throwable error = task.getException();
            String detail = error == null || error.getMessage() == null || error.getMessage().isBlank()
                    ? "Impossible d'envoyer le code OTP."
                    : error.getMessage();
            setFeedback(detail, true);
        });

        Thread thread = new Thread(task, "otp-sms-sender");
        thread.setDaemon(true);
        thread.start();
    }

    private void setControlsDisabled(boolean disabled) {
        verifyButton.setDisable(disabled);
        resendButton.setDisable(disabled);
        otpField.setDisable(disabled);
    }

    private void setFeedback(String message, boolean error) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("login-error", "login-success");
        if (!message.isBlank()) {
            feedbackLabel.getStyleClass().add(error ? "login-error" : "login-success");
        }
    }

    private static String maskPhone(String phoneNumber) {
        String value = Optional.ofNullable(phoneNumber).orElse("").trim();
        if (value.length() <= 4) {
            return value.isBlank() ? "telephone du compte" : value;
        }
        return "***" + value.substring(value.length() - 4);
    }
}
