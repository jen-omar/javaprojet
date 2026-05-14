package tn.esprit.mythoria.utils;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

public final class EmailUtil {

    // 🔥 HARDCODED CONFIG (change these values)
    private static final String HOST = "sandbox.smtp.mailtrap.io";
    private static final String PORT = "587";
    private static final String USERNAME ="49302c9a5554d6";
    private static final String PASSWORD = "195c458f2e7e0a";
    private static final String FROM = "no-reply@mythoria.local";

    private static final boolean AUTH = true;
    private static final boolean STARTTLS = true;

    private EmailUtil() {
    }
    public static boolean isConfigured() {
        return !isBlank(HOST)
                && !isBlank(PORT)
                && !isBlank(USERNAME)
                && !isBlank(PASSWORD)
                && !isBlank(FROM);
    }
    public static void sendEmail(String recipient, String subject, String body) throws MessagingException {
        if (isBlank(recipient)) {
            throw new IllegalArgumentException("Le destinataire email est vide.");
        }

        Session session = buildSession();

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        message.setSubject(subject, StandardCharsets.UTF_8.name());
        message.setText(body, StandardCharsets.UTF_8.name());

        Transport.send(message);
    }

    public static int sendBulkEmail(List<String> recipients, String subject, String body) throws MessagingException {
        if (recipients == null || recipients.isEmpty()) {
            return 0;
        }

        int sentCount = 0;
        for (String recipient : recipients) {
            if (!isBlank(recipient)) {
                sendEmail(recipient.trim(), subject, body);
                sentCount++;
            }
        }
        return sentCount;
    }

    private static Session buildSession() {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", HOST);
        properties.put("mail.smtp.port", PORT);
        properties.put("mail.smtp.auth", String.valueOf(AUTH));
        properties.put("mail.smtp.starttls.enable", String.valueOf(STARTTLS));
        properties.put("mail.smtp.ssl.trust", HOST);

        return Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}