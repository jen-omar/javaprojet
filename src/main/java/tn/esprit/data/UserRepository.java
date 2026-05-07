package tn.esprit.data;

import tn.esprit.controllers.ValidationUtils;
import tn.esprit.Models.User;
import tn.esprit.controllers.services.EmailNotificationService;
import tn.esprit.util.MyConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class UserRepository {
    private static final Logger LOG = Logger.getLogger(UserRepository.class.getName());

    public UserRepository() {
    }

    public Optional<User> authenticate(String username, String rawPassword) {
        return authenticateFromDatabase(username, rawPassword);
    }

    public User registerUser(String username, String email, String rawPassword, String firstName, String lastName, String phoneNumber) {
        return registerUserInDatabase(username, email, rawPassword, firstName, lastName, phoneNumber)
                .orElseThrow(() -> new IllegalStateException("Could not create user in database"));
    }

    public Optional<User> findById(String userId) {
        return findUserInDatabase(userId);
    }

    public boolean updateScoreAndLevel(String userId, int score, String level) {
        int normalizedScore = Math.max(0, score);
        String normalizedLevel = Optional.ofNullable(level).orElse(rankFromScore(normalizedScore)).trim();
        if (normalizedLevel.isBlank()) {
            normalizedLevel = rankFromScore(normalizedScore);
        }

        return updateScoreAndLevelInDatabase(userId, normalizedScore, normalizedLevel);
    }

    private Optional<User> authenticateFromDatabase(String login, String rawPassword) {
        String sql = """
                SELECT *
                FROM user
                WHERE est_valide = 1
                  AND (LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?))
                LIMIT 1
                """;

        try (Connection connection = MyConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            statement.setString(2, login);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                String storedPassword = Optional.ofNullable(rs.getString("password")).orElse("");
                if (!passwordsMatch(rawPassword, hashPassword(rawPassword), storedPassword)) {
                    return Optional.empty();
                }

                return Optional.of(mapDatabaseUser(rs, storedPassword, true));
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Database authentication failed");
            return Optional.empty();
        }
    }

    private Optional<User> registerUserInDatabase(String username, String email, String rawPassword, String firstName, String lastName, String phoneNumber) {
        String normalizedUsername = ValidationUtils.requireUsername(username);
        String normalizedEmail = ValidationUtils.requireEmail(email);
        String validatedPassword = ValidationUtils.requireStrongPassword(rawPassword);
        String normalizedFirstName = ValidationUtils.optionalName(firstName, "Prenom");
        String normalizedLastName = ValidationUtils.optionalName(lastName, "Nom");
        String normalizedPhone = ValidationUtils.optionalPhone(phoneNumber);

        try (Connection connection = MyConnection.getInstance().getConnection()) {
            ensurePhoneNumberColumn(connection);
            ensureLevelColumn(connection);
            if (databaseUserExists(connection, normalizedUsername, normalizedEmail)) {
                throw new IllegalArgumentException("Username or email already exists.");
            }

            int nextId = nextUserId(connection);
            String sql = """
                    INSERT INTO user (id, email, username, roles, password, prenom, nom, phone_number, est_valide, created_at, score, level)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, NOW(), 0, ?)
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, nextId);
                statement.setString(2, normalizedEmail);
                statement.setString(3, normalizedUsername);
                statement.setString(4, "[\"ROLE_USER\"]");
                statement.setString(5, BCrypt.hashpw(validatedPassword, BCrypt.gensalt()));
                statement.setString(6, normalizedFirstName);
                statement.setString(7, normalizedLastName);
                statement.setString(8, normalizedPhone);
                statement.setString(9, rankFromScore(0));
                statement.executeUpdate();
            }

            return findUserInDatabase(String.valueOf(nextId));
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to register SQL user " + normalizedUsername);
            return Optional.empty();
        }
    }

    public User updateProfile(User currentUser, String email, String firstName, String lastName, String phoneNumber) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Current user is required");
        }

        return updateProfileInDatabase(currentUser, email, firstName, lastName, phoneNumber)
                .orElseThrow(() -> new IllegalStateException("Unable to update SQL profile"));
    }

    public Optional<User> reloadUser(User currentUser) {
        if (currentUser == null) {
            return Optional.empty();
        }

        return findUserInDatabase(currentUser.id());
    }

    public boolean updatePassword(User currentUser, String rawPassword, EmailNotificationService emailNotificationService) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Current user is required");
        }
        String validatedPassword = ValidationUtils.requireStrongPassword(rawPassword);

        boolean updated = updatePasswordInDatabase(currentUser, validatedPassword);
        if (updated && emailNotificationService != null) {
            emailNotificationService.sendPasswordChangeAlert(currentUser);
        }
        return updated;
    }

    private Optional<User> updateProfileInDatabase(User currentUser, String email, String firstName, String lastName, String phoneNumber) {
        String sql = """
                UPDATE user
                SET email = ?, prenom = ?, nom = ?, phone_number = ?
                WHERE id = ?
                """;

        try (Connection connection = MyConnection.getInstance().getConnection()) {
            ensurePhoneNumberColumn(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, ValidationUtils.requireEmail(email));
                statement.setString(2, ValidationUtils.optionalName(firstName, "Prenom"));
                statement.setString(3, ValidationUtils.optionalName(lastName, "Nom"));
                statement.setString(4, ValidationUtils.optionalPhone(phoneNumber));
                statement.setInt(5, Integer.parseInt(currentUser.id()));
                statement.executeUpdate();
            }

            return findUserInDatabase(currentUser.id());
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to update SQL profile");
            return Optional.empty();
        }
    }

    private Optional<User> findUserInDatabase(String userId) {
        String sql = """
                SELECT *
                FROM user
                WHERE id = ?
                LIMIT 1
                """;

        try (Connection connection = MyConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Integer.parseInt(userId));

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapDatabaseUser(rs, Optional.ofNullable(rs.getString("password")).orElse(""), true));
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to reload SQL user");
            return Optional.empty();
        }
    }

    private boolean updatePasswordInDatabase(User currentUser, String validatedPassword) {
        String sql = """
                UPDATE user
                SET password = ?
                WHERE id = ?
                """;

        try (Connection connection = MyConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, BCrypt.hashpw(validatedPassword, BCrypt.gensalt()));
            statement.setInt(2, Integer.parseInt(currentUser.id()));
            return statement.executeUpdate() > 0;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to update SQL password");
            return false;
        }
    }

    private User mapDatabaseUser(ResultSet rs, String storedPassword, boolean databaseBacked) throws java.sql.SQLException {
        String firstName = Optional.ofNullable(rs.getString("prenom")).orElse("").trim();
        String lastName = Optional.ofNullable(rs.getString("nom")).orElse("").trim();
        String username = Optional.ofNullable(rs.getString("username")).orElse(rs.getString("email"));
        String displayName = (firstName + " " + lastName).trim();
        if (displayName.isBlank()) {
            displayName = Optional.ofNullable(rs.getString("username")).orElse("").trim();
        }

        int points = rs.getInt("score");
        String level = Optional.ofNullable(readStringSafely(rs, "level")).orElse("").trim();
        if (level.isBlank()) {
            level = rankFromScore(points);
        }
        String role = extractRole(rs, username);
        return new User(
                String.valueOf(rs.getInt("id")),
                username,
                displayName,
                storedPassword,
                level,
                points,
                Optional.ofNullable(rs.getString("email")).orElse(""),
                firstName,
                lastName,
                Optional.ofNullable(readStringSafely(rs, "phone_number")).orElse(""),
                role,
                databaseBacked
        );
    }

    private static String readStringSafely(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String rankFromScore(int score) {
        if (score >= 1000) {
            return "L\u00e9gende";
        }
        if (score >= 600) {
            return "Expert";
        }
        if (score >= 300) {
            return "Avanc\u00e9";
        }
        if (score >= 100) {
            return "Actif";
        }
        return "D\u00e9butant";
    }



    public List<User> findAllUsers() {
        String sql = """
                SELECT *
                FROM user
                ORDER BY id DESC
                """;

        List<User> result = new ArrayList<>();
        try (Connection connection = MyConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(mapDatabaseUser(rs, Optional.ofNullable(rs.getString("password")).orElse(""), true));
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to load all users");
        }
        return result;
    }

    public Optional<User> adminUpdateUser(String userId, String email, String firstName, String lastName, String phoneNumber, int score, String role) {
        String sql = """
                UPDATE user
                SET email = ?, prenom = ?, nom = ?, phone_number = ?, score = ?, level = ?, roles = ?
                WHERE id = ?
                """;

        try (Connection connection = MyConnection.getInstance().getConnection()) {
            ensurePhoneNumberColumn(connection);
            ensureLevelColumn(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, ValidationUtils.requireEmail(email));
                statement.setString(2, ValidationUtils.optionalName(firstName, "Prenom"));
                statement.setString(3, ValidationUtils.optionalName(lastName, "Nom"));
                statement.setString(4, ValidationUtils.optionalPhone(phoneNumber));
                statement.setInt(5, score);
                statement.setString(6, rankFromScore(score));
                statement.setString(7, toDatabaseRoles(ValidationUtils.requireRole(role)));
                statement.setInt(8, Integer.parseInt(userId));
                int updatedRows = statement.executeUpdate();
                if (updatedRows > 0) {
                    return findUserInDatabase(userId);
                }
                return Optional.empty();
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to admin update user " + userId);
            throw new IllegalStateException("Database error while updating user: " + ex.getMessage(), ex);
        }
    }

    public boolean adminDeleteUser(String userId) {
        String sql = "DELETE FROM user WHERE id = ?";
        try (Connection connection = MyConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Integer.parseInt(userId));
            return statement.executeUpdate() > 0;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to delete user " + userId);
            return false;
        }
    }

    private boolean databaseUserExists(Connection connection, String username, String email) throws Exception {
        String sql = """
                SELECT id
                FROM user
                WHERE LOWER(username) = LOWER(?)
                   OR LOWER(email) = LOWER(?)
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, email);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int nextUserId(Connection connection) throws Exception {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM user";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("next_id");
            }
            return 1;
        }
    }

    private void ensurePhoneNumberColumn(Connection connection) {
        String sql = "ALTER TABLE user ADD COLUMN phone_number VARCHAR(30)";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException ex) {
            String state = Optional.ofNullable(ex.getSQLState()).orElse("");
            boolean duplicateColumn = "42S21".equals(state)
                    || Optional.ofNullable(ex.getMessage()).orElse("").toLowerCase().contains("duplicate column");
            if (!duplicateColumn) {
                LOG.log(Level.WARNING, ex, () -> "Unable to ensure phone_number column");
            }
        }
    }

    private void ensureLevelColumn(Connection connection) {
        String sql = "ALTER TABLE user ADD COLUMN level VARCHAR(30) DEFAULT 'D\u00e9butant'";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException ex) {
            String state = Optional.ofNullable(ex.getSQLState()).orElse("");
            boolean duplicateColumn = "42S21".equals(state)
                    || Optional.ofNullable(ex.getMessage()).orElse("").toLowerCase().contains("duplicate column");
            if (!duplicateColumn) {
                LOG.log(Level.WARNING, ex, () -> "Unable to ensure level column");
            }
        }
    }

    private boolean updateScoreAndLevelInDatabase(String userId, int score, String level) {
        String sql = """
                UPDATE user
                SET score = ?, level = ?
                WHERE id = ?
                """;

        try (Connection connection = MyConnection.getInstance().getConnection()) {
            ensureLevelColumn(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, score);
                statement.setString(2, level);
                statement.setInt(3, Integer.parseInt(userId));
                return statement.executeUpdate() > 0;
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to update SQL score for user " + userId);
            return false;
        }
    }

    private static User withScoreAndLevel(User user, int score, String level, boolean databaseBacked) {
        return new User(
                user.id(),
                user.username(),
                user.displayName(),
                user.passwordHash(),
                level,
                score,
                user.email(),
                user.firstName(),
                user.lastName(),
                user.phoneNumber(),
                user.role(),
                databaseBacked
        );
    }

    private static String normalizeRole(String value) {
        String normalized = Optional.ofNullable(value).orElse("user").trim().toLowerCase();
        return normalized.isBlank() ? "user" : normalized;
    }

    private static String toDatabaseRoles(String role) {
        return switch (normalizeRole(role)) {
            case "admin" -> "[\"ROLE_ADMIN\"]";
            case "author" -> "[\"ROLE_AUTHOR\"]";
            case "client" -> "[\"ROLE_CLIENT\"]";
            default -> "[\"ROLE_USER\"]";
        };
    }

    private static String extractRole(ResultSet rs, String username) {
        String rolesJson = Optional.ofNullable(readStringSafely(rs, "roles")).orElse("").toUpperCase();
        if (rolesJson.contains("ROLE_ADMIN") || rolesJson.contains("\"ADMIN\"") || rolesJson.contains("ADMIN")) {
            return "admin";
        }
        if (rolesJson.contains("ROLE_AUTHOR") || rolesJson.contains("\"AUTHOR\"") || rolesJson.contains("AUTHOR")) {
            return "author";
        }
        if (rolesJson.contains("ROLE_CLIENT") || rolesJson.contains("\"CLIENT\"") || rolesJson.contains("CLIENT")) {
            return "client";
        }
        if (rolesJson.contains("ROLE_USER") || rolesJson.contains("\"USER\"") || rolesJson.contains("USER")) {
            return "user";
        }

        String simpleRole = Optional.ofNullable(readStringSafely(rs, "role")).orElse("").trim();
        if (!simpleRole.isBlank()) {
            return normalizeRole(simpleRole);
        }

        if ("admin".equalsIgnoreCase(Optional.ofNullable(username).orElse("").trim())) {
            return "admin";
        }
        return "user";
    }

    private static String normalizeEmail(String value) {
        return Optional.ofNullable(value).orElse("").trim().toLowerCase();
    }

    private static String normalizeProfileValue(String value) {
        return Optional.ofNullable(value).orElse("").trim();
    }

    private static String normalize(String value) {
        return Optional.ofNullable(value).orElse("").trim().toLowerCase();
    }

    private static String hashPassword(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(Optional.ofNullable(value).orElse("").getBytes(StandardCharsets.UTF_8));
            try (Formatter formatter = new Formatter()) {
                for (byte b : encodedHash) {
                    formatter.format("%02x", b);
                }
                return formatter.toString();
            }
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private static boolean passwordsMatch(String rawPassword, String rawSha256, String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (isBcryptHash(storedPassword)) {
            String normalizedHash = storedPassword.startsWith("$2y$")
                    ? "$2a$" + storedPassword.substring(4)
                    : storedPassword;
            return BCrypt.checkpw(rawPassword, normalizedHash);
        }

        if (storedPassword.equals(rawPassword)) {
            return true;
        }

        return storedPassword.equalsIgnoreCase(rawSha256);
    }

    private static boolean isBcryptHash(String storedPassword) {
        return storedPassword.startsWith("$2a$")
                || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$");
    }

    private static Path defaultStorageFile() {
        return Path.of(System.getProperty("user.dir"), ".mythoria", "users.json");
    }
}
