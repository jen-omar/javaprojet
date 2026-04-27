package com.example.mythoriadesktop.data;

import com.example.mythoriadesktop.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NotificationDAO {
    private static final Logger LOG = Logger.getLogger(NotificationDAO.class.getName());

    private final DatabaseConnection databaseConnection = new DatabaseConnection(DatabaseConfig.fromEnvironment());

    public void save(User user, String type, String recipientEmail, String subject, String message, String status) {
        if (user == null || !user.databaseBacked()) {
            return;
        }

        String sql = """
                INSERT INTO email_notifications (user_id, notification_type, recipient_email, subject, message, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = databaseConnection.getConnection()) {
            ensureTable(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, Integer.parseInt(user.id()));
                statement.setString(2, Optional.ofNullable(type).orElse(""));
                statement.setString(3, Optional.ofNullable(recipientEmail).orElse(""));
                statement.setString(4, Optional.ofNullable(subject).orElse(""));
                statement.setString(5, Optional.ofNullable(message).orElse(""));
                statement.setString(6, Optional.ofNullable(status).orElse("SENT"));
                statement.executeUpdate();
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Unable to save email notification history");
        }
    }

    private static void ensureTable(Connection connection) throws Exception {
        String createTableSql = """
                CREATE TABLE IF NOT EXISTS email_notifications (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    notification_type VARCHAR(50) NOT NULL,
                    recipient_email VARCHAR(150) NOT NULL,
                    subject VARCHAR(255) NOT NULL,
                    message TEXT NOT NULL,
                    status VARCHAR(30) DEFAULT 'SENT',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_email_notifications_user_id (user_id),
                    INDEX idx_email_notifications_type (notification_type)
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSql);
        }
    }
}
