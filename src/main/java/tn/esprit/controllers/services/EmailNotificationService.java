package tn.esprit.controllers.services;

import tn.esprit.data.NotificationDAO;
import tn.esprit.Models.User;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class EmailNotificationService {
    private static final Logger LOG = Logger.getLogger(EmailNotificationService.class.getName());

    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "email-notification-sender");
        thread.setDaemon(true);
        return thread;
    });

    private final MailConfig mailConfig;

    public EmailNotificationService() {
        this.mailConfig = MailConfig.load();
    }

    public void sendLoginAlert(User user) {
        NotificationTemplate.EmailMessage email = NotificationTemplate.loginAlert(user, LocalDateTime.now());
        sendAsync(user, NotificationTemplate.LOGIN_ALERT, email);
    }

    public void sendRoleChangeAlert(User user, String oldRole, String newRole) {
        if (sameRole(oldRole, newRole)) {
            return;
        }
        NotificationTemplate.EmailMessage email = NotificationTemplate.roleChangeAlert(user, oldRole, newRole);
        sendAsync(user, NotificationTemplate.ROLE_CHANGE_ALERT, email);
    }

    public void sendPasswordChangeAlert(User user) {
        NotificationTemplate.EmailMessage email = NotificationTemplate.passwordChangeAlert(user);
        sendAsync(user, NotificationTemplate.PASSWORD_CHANGE_ALERT, email);
    }

    private void sendAsync(User user, String type, NotificationTemplate.EmailMessage email) {
        if (user == null || user.email().isBlank()) {
            return;
        }

        executor.submit(() -> {
            String status = "SENT";
            try {
                sendEmail(user.email(), email.subject(), email.message());
            } catch (Exception ex) {
                status = "FAILED";
                LOG.log(Level.WARNING, ex, () -> "Email notification failed for type " + type);
            } finally {
                notificationDAO.save(user, type, user.email(), email.subject(), email.message(), status);
            }
        });
    }

    private void sendEmail(String recipientEmail, String subject, String body) throws MessagingException {
        if (!mailConfig.isConfigured()) {
            throw new IllegalStateException("Mail configuration is missing.");
        }

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", mailConfig.host());
        properties.put("mail.smtp.port", mailConfig.port());

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailConfig.username(), mailConfig.password());
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(mailConfig.from()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject(subject);
        message.setText(body);
        Transport.send(message);
    }

    private static boolean sameRole(String oldRole, String newRole) {
        return Optional.ofNullable(oldRole).orElse("").trim().equalsIgnoreCase(
                Optional.ofNullable(newRole).orElse("").trim()
        );
    }

    private record MailConfig(
            String host,
            String port,
            String username,
            String password,
            String from
    ) {
        private boolean isConfigured() {
            return !host.isBlank()
                    && !port.isBlank()
                    && !username.isBlank()
                    && !password.isBlank()
                    && !from.isBlank()
                    && !password.startsWith("YOUR_");
        }

        private static MailConfig load() {
            Properties properties = loadProperties();
            return new MailConfig(
                    read("MAIL_HOST", "mail.host", properties),
                    read("MAIL_PORT", "mail.port", properties),
                    read("MAIL_USERNAME", "mail.username", properties),
                    read("MAIL_PASSWORD", "mail.password", properties),
                    read("MAIL_FROM", "mail.from", properties)
            );
        }

        private static String read(String envKey, String propertyKey, Properties properties) {
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

        private static InputStream resolveConfigStream() throws IOException {
            InputStream classpathStream = EmailNotificationService.class.getClassLoader().getResourceAsStream("config.properties");
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
}
