package com.example.mythoriadesktop.services;

import com.example.mythoriadesktop.data.DatabaseConfig;
import com.example.mythoriadesktop.data.DatabaseConnection;
import com.example.mythoriadesktop.model.User;
import com.example.mythoriadesktop.model.UserActivity;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class UserActivityService {
    private static final Logger LOG = Logger.getLogger(UserActivityService.class.getName());

    private final DatabaseConnection databaseConnection;

    public UserActivityService() {
        this(DatabaseConfig.fromEnvironment());
    }

    public UserActivityService(DatabaseConfig databaseConfig) {
        this.databaseConnection = new DatabaseConnection(databaseConfig);
    }

    public UserActivity analyze(User user) {
        int profileCompletionPercent = calculateProfileCompletion(user);

        if (user == null || !user.databaseBacked()) {
            return new UserActivity(0, profileCompletionPercent, 0, 0, 0, 0, 0, 0, 0);
        }

        int userId = parseUserId(user.id());
        if (userId <= 0) {
            return new UserActivity(0, profileCompletionPercent, 0, 0, 0, 0, 0, 0, 0);
        }

        try (Connection connection = databaseConnection.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return new UserActivity(
                    readLoginCount(connection, metaData, userId),
                    profileCompletionPercent,
                    countFirstAvailable(connection, metaData, userId, List.of("artwork", "artworks", "oeuvre", "oeuvres"), List.of("user_id", "created_by", "author_id")),
                    countFirstAvailable(connection, metaData, userId, List.of("book", "books", "livre", "livres"), List.of("user_id", "author_id", "created_by")),
                    countFirstAvailable(connection, metaData, userId, List.of("event_participation", "event_participations", "participation_evenement", "ticket"), List.of("user_id", "participant_id")),
                    countFirstAvailable(connection, metaData, userId, List.of("purchase", "purchases", "commande", "orders", "transaction"), List.of("user_id", "buyer_id", "client_id")),
                    countFirstAvailable(connection, metaData, userId, List.of("comment", "comments", "commentaire", "commentaires"), List.of("user_id", "author_id", "created_by")),
                    countFirstAvailable(connection, metaData, userId, List.of("collaboration", "collaborations", "collaborator", "collaborators"), List.of("user_id", "creator_id", "collaborator_id")),
                    countFirstAvailable(connection, metaData, userId, List.of("report", "reports", "signalement", "signalements"), List.of("user_id", "reported_user_id", "author_id"))
            );
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Unable to analyze activity for user " + user.id());
            return new UserActivity(0, profileCompletionPercent, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    private int readLoginCount(Connection connection, DatabaseMetaData metaData, int userId) throws Exception {
        if (columnExists(metaData, "user", "login_count")) {
            String sql = "SELECT COALESCE(" + quoteIdentifier("login_count") + ", 0) AS login_count FROM "
                    + quoteIdentifier("user") + " WHERE " + quoteIdentifier("id") + " = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, userId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return Math.max(0, rs.getInt("login_count"));
                    }
                }
            }
        }

        return countFirstAvailable(connection, metaData, userId,
                List.of("login_history", "login_logs", "auth_log", "auth_logs"),
                List.of("user_id"));
    }

    private int countFirstAvailable(Connection connection, DatabaseMetaData metaData, int userId, List<String> tables, List<String> userColumns) throws Exception {
        for (String table : tables) {
            if (!tableExists(metaData, table)) {
                continue;
            }
            for (String userColumn : userColumns) {
                if (!columnExists(metaData, table, userColumn)) {
                    continue;
                }
                return countRows(connection, table, userColumn, userId);
            }
        }
        return 0;
    }

    private int countRows(Connection connection, String table, String userColumn, int userId) throws Exception {
        String sql = "SELECT COUNT(*) AS total FROM " + quoteIdentifier(table)
                + " WHERE " + quoteIdentifier(userColumn) + " = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Math.max(0, rs.getInt("total"));
                }
            }
        }
        return 0;
    }

    private static String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private boolean tableExists(DatabaseMetaData metaData, String tableName) throws Exception {
        Set<String> candidates = Set.of(tableName, tableName.toLowerCase(Locale.ROOT), tableName.toUpperCase(Locale.ROOT));
        for (String candidate : candidates) {
            try (ResultSet rs = metaData.getTables(null, null, candidate, new String[]{"TABLE"})) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean columnExists(DatabaseMetaData metaData, String tableName, String columnName) throws Exception {
        Set<String> tableCandidates = Set.of(tableName, tableName.toLowerCase(Locale.ROOT), tableName.toUpperCase(Locale.ROOT));
        Set<String> columnCandidates = Set.of(columnName, columnName.toLowerCase(Locale.ROOT), columnName.toUpperCase(Locale.ROOT));
        for (String tableCandidate : tableCandidates) {
            for (String columnCandidate : columnCandidates) {
                try (ResultSet rs = metaData.getColumns(null, null, tableCandidate, columnCandidate)) {
                    if (rs.next()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int calculateProfileCompletion(User user) {
        if (user == null) {
            return 0;
        }

        int completed = 0;
        int total = 5;
        if (!isBlank(user.username())) {
            completed++;
        }
        if (!isBlank(user.email())) {
            completed++;
        }
        if (!isBlank(user.firstName())) {
            completed++;
        }
        if (!isBlank(user.lastName())) {
            completed++;
        }
        if (!isBlank(user.phoneNumber())) {
            completed++;
        }
        return (completed * 100) / total;
    }

    private static boolean isBlank(String value) {
        return Optional.ofNullable(value).orElse("").trim().isBlank();
    }

    private static int parseUserId(String userId) {
        try {
            return Integer.parseInt(Optional.ofNullable(userId).orElse("").trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
}
