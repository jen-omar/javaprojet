package com.example.mythoriadesktop.services;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.exception.AuthenticationException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public final class TwilioSmsService {
    private final String accountSid;
    private final String authToken;
    private final String fromPhoneNumber;

    public TwilioSmsService() {
        Properties properties = loadProperties();
        this.accountSid = readSecret("TWILIO_ACCOUNT_SID", "twilio.account.sid", properties);
        this.authToken = readSecret("TWILIO_AUTH_TOKEN", "twilio.auth.token", properties);
        this.fromPhoneNumber = readSecret("TWILIO_PHONE_NUMBER", "twilio.phone.number", properties);
    }

    public void sendOtp(String destinationPhoneNumber, String otpCode) {
        String to = Optional.ofNullable(destinationPhoneNumber).orElse("").trim().replace(" ", "");
        if (to.isBlank()) {
            throw new IllegalArgumentException("User phone number is required for OTP.");
        }
        if (!isConfigured()) {
            throw new IllegalStateException("Twilio configuration is missing.");
        }

        try {
            Twilio.init(accountSid, authToken);
            Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(fromPhoneNumber.replace(" ", "")),
                    "Mythoria verification code: " + otpCode + ". This code expires in 5 minutes."
            ).create();
        } catch (AuthenticationException ex) {
            throw new IllegalStateException("Twilio authentication failed. Check Account SID and Auth Token.", ex);
        } catch (ApiException ex) {
            throw new IllegalStateException(toSafeTwilioMessage(ex), ex);
        }
    }

    public boolean isConfigured() {
        return !accountSid.isBlank() && !authToken.isBlank() && !fromPhoneNumber.isBlank()
                && !accountSid.startsWith("YOUR_")
                && !authToken.startsWith("YOUR_")
                && !fromPhoneNumber.startsWith("YOUR_");
    }

    private static String readSecret(String envKey, String propertyKey, Properties properties) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        return Optional.ofNullable(properties.getProperty(propertyKey)).orElse("").trim();
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream stream = resolveConfigStream()) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load config.properties.", ex);
        }
        return properties;
    }

    private static String toSafeTwilioMessage(ApiException ex) {
        String message = Optional.ofNullable(ex.getMessage()).orElse("").toLowerCase();
        if (message.contains("unverified") || message.contains("not a verified")) {
            return "Twilio trial account can only send SMS to verified recipient phone numbers.";
        }
        if (message.contains("not a valid phone number") || message.contains("invalid")) {
            return "Twilio rejected the phone number. Use international format like +21612345678.";
        }
        return "Twilio SMS request failed. Check the sender number, recipient number, and Twilio account status.";
    }

    private static InputStream resolveConfigStream() throws IOException {
        InputStream classpathStream = TwilioSmsService.class.getClassLoader().getResourceAsStream("config.properties");
        if (classpathStream != null) {
            return classpathStream;
        }

        Path fileSystemPath = Path.of(System.getProperty("user.dir"), "config.properties");
        if (Files.exists(fileSystemPath)) {
            return Files.newInputStream(fileSystemPath);
        }
        return null;
    }
}
