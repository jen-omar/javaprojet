package com.example.mythoriadesktop.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ChatHistoryDAO {
    private static final Logger LOG = Logger.getLogger(ChatHistoryDAO.class.getName());

    private final DatabaseConnection databaseConnection = new DatabaseConnection(DatabaseConfig.fromEnvironment());

    public List<String> findRecentUserMessages(int userId, int limit) {
        if (userId <= 0 || limit <= 0) {
            return List.of();
        }

        String sql = """
                SELECT user_message
                FROM chat_history
                WHERE user_id = ?
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """;

        try (Connection connection = databaseConnection.getConnection()) {
            ensureTable(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, userId);
                statement.setInt(2, Math.min(limit, 25));
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<String> messages = new ArrayList<>();
                    while (resultSet.next()) {
                        String message = Optional.ofNullable(resultSet.getString("user_message")).orElse("").trim();
                        if (!message.isBlank()) {
                            messages.add(message);
                        }
                    }
                    return messages;
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Unable to read chat history");
            return List.of();
        }
    }

    public void save(int userId, String userMessage, String aiResponse) {
        if (userId <= 0) {
            return;
        }

        String sql = """
                INSERT INTO chat_history (user_id, user_message, ai_response)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = databaseConnection.getConnection()) {
            ensureTable(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, userId);
                statement.setString(2, Optional.ofNullable(userMessage).orElse(""));
                statement.setString(3, Optional.ofNullable(aiResponse).orElse(""));
                statement.executeUpdate();
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Unable to save chat history");
        }
    }

    private static void ensureTable(Connection connection) throws Exception {
        String createTableSql = """
                CREATE TABLE IF NOT EXISTS chat_history (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    user_message TEXT NOT NULL,
                    ai_response TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_chat_history_user_id (user_id)
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSql);
        }
    }
}
