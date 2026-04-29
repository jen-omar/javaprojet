package com.example.mythoriadesktop.data;

import com.example.mythoriadesktop.ValidationUtils;
import com.example.mythoriadesktop.model.User;
import com.example.mythoriadesktop.services.EmailNotificationService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
    private static final Type USER_LIST_TYPE = TypeToken.getParameterized(List.class, User.class).getType();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path storageFile;
    private final List<User> users = new ArrayList<>();
    private final DatabaseConfig databaseConfig;
    private final DatabaseConnection databaseConnection;

    public UserRepository() {
        this(defaultStorageFile(), DatabaseConfig.fromEnvironment());
    }

    public UserRepository(Path storageFile) {
        this(storageFile, DatabaseConfig.fromEnvironment());
    }

    public UserRepository(Path storageFile, DatabaseConfig databaseConfig) {
        this.storageFile = storageFile;
        this.databaseConfig = databaseConfig;
        this.databaseConnection = new DatabaseConnection(databaseConfig);
        loadFromDisk();
    }

    public Optional<User> authenticate(String username, String rawPassword) {
        if (databaseConfig.isConfigured()) {
            Optional<User> user = authenticateFromDatabase(username, rawPassword);
            if (user.isPresent()) {
                return user;
            }
        }

        String normalizedUsername = normalize(username);
        String passwordHash = hashPassword(rawPassword);
        return users.stream()
                .filter(user -> user.username().equals(normalizedUsername) && passwordsMatch(rawPassword, passwordHash, user.passwordHash()))
                .findFirst();
    }

    public User registerUser(String username, String email, String rawPassword, String firstName, String lastName, String phoneNumber) {
        if (databaseConfig.isConfigured()) {
            Optional<User> created = registerUserInDatabase(username, email, rawPassword, firstName, lastName, phoneNumber);
            if (created.isPresent()) {
                return created.get();
            }
        }

        return registerUserInLocalStorage(username, email, rawPassword, firstName, lastName, phoneNumber);
    }

    public Optional<User> findById(String userId) {
        if (databaseConfig.isConfigured()) {
            Optional<User> databaseUser = findUserInDatabase(userId);
            if (databaseUser.isPresent()) {
                return databaseUser;
            }
        }

        return users.stream()
                .filter(user -> user.id().equals(userId))
                .findFirst();
    }

    public boolean updateScoreAndLevel(String userId, int score, String level) {
        int normalizedScore = Math.max(0, score);
        String normalizedLevel = Optional.ofNullable(level).orElse(rankFromScore(normalizedScore)).trim();
        if (normalizedLevel.isBlank()) {
            normalizedLevel = rankFromScore(normalizedScore);
        }

        if (databaseConfig.isConfigured() && updateScoreAndLevelInDatabase(userId, normalizedScore, normalizedLevel)) {
            return true;
        }

        for (int i = 0; i < users.size(); i++) {
            User existing = users.get(i);
            if (!existing.id().equals(userId)) {
                continue;
            }

            users.set(i, withScoreAndLevel(existing, normalizedScore, normalizedLevel, false));
            saveToDisk();
            return true;
        }

        return false;
    }

    private Optional<User> authenticateFromDatabase(String login, String rawPassword) {
        String sql = """
                SELECT *
                FROM user
                WHERE est_valide = 1
                  AND (LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?))
                LIMIT 1
                """;

        try (Connection connection = databaseConnection.getConnection();
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
            LOG.log(Level.WARNING, ex, () -> "Database authentication failed, falling back to local users");
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

        try (Connection connection = databaseConnection.getConnection()) {
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

    private User registerUserInLocalStorage(String username, String email, String rawPassword, String firstName, String lastName, String phoneNumber) {
        String normalizedUsername = ValidationUtils.requireUsername(username);
        String normalizedEmail = ValidationUtils.requireEmail(email);
        String validatedPassword = ValidationUtils.requireStrongPassword(rawPassword);
        String normalizedFirstName = ValidationUtils.optionalName(firstName, "Prenom");
        String normalizedLastName = ValidationUtils.optionalName(lastName, "Nom");
        String normalizedPhone = ValidationUtils.optionalPhone(phoneNumber);

        boolean exists = users.stream().anyMatch(user ->
                user.username().equals(normalizedUsername) || user.email().equalsIgnoreCase(normalizedEmail));
        if (exists) {
            throw new IllegalArgumentException("Username or email already exists.");
        }

        String displayName = (normalizedFirstName + " " + normalizedLastName).trim();
        if (displayName.isBlank()) {
            displayName = normalizedUsername;
        }

        User user = new User(
                java.util.UUID.randomUUID().toString(),
                normalizedUsername,
                displayName,
                BCrypt.hashpw(validatedPassword, BCrypt.gensalt()),
                rankFromScore(0),
                0,
                normalizedEmail,
                normalizedFirstName,
                normalizedLastName,
                normalizedPhone,
                "user",
                false
        );
        users.add(user);
        saveToDisk();
        return user;
    }

    public User updateProfile(User currentUser, String email, String firstName, String lastName, String phoneNumber) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Current user is required");
        }

        if (currentUser.databaseBacked()) {
            return updateProfileInDatabase(currentUser, email, firstName, lastName, phoneNumber)
                    .orElseThrow(() -> new IllegalStateException("Unable to update SQL profile"));
        }

        return updateProfileInLocalStorage(currentUser, email, firstName, lastName, phoneNumber);
    }

    public Optional<User> reloadUser(User currentUser) {
        if (currentUser == null) {
            return Optional.empty();
        }

        if (currentUser.databaseBacked()) {
            return findUserInDatabase(currentUser.id());
        }

        return users.stream()
                .filter(user -> user.id().equals(currentUser.id()))
                .findFirst();
    }

    public boolean updatePassword(User currentUser, String rawPassword, EmailNotificationService emailNotificationService) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Current user is required");
        }
        String validatedPassword = ValidationUtils.requireStrongPassword(rawPassword);

        boolean updated = currentUser.databaseBacked()
                ? updatePasswordInDatabase(currentUser, validatedPassword)
                : updatePasswordInLocalStorage(currentUser, validatedPassword);
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

        try (Connection connection = databaseConnection.getConnection()) {
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

    private User updateProfileInLocalStorage(User currentUser, String email, String firstName, String lastName, String phoneNumber) {
        for (int i = 0; i < users.size(); i++) {
            User existing = users.get(i);
            if (!existing.id().equals(currentUser.id())) {
                continue;
            }

            User updated = mergeProfile(existing, email, firstName, lastName, phoneNumber, false);
            users.set(i, updated);
            saveToDisk();
            return updated;
        }

        User updated = mergeProfile(currentUser, email, firstName, lastName, phoneNumber, false);
        users.add(updated);
        saveToDisk();
        return updated;
    }

    private Optional<User> findUserInDatabase(String userId) {
        String sql = """
                SELECT *
                FROM user
                WHERE id = ?
                LIMIT 1
                """;

        try (Connection connection = databaseConnection.getConnection();
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

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, BCrypt.hashpw(validatedPassword, BCrypt.gensalt()));
            statement.setInt(2, Integer.parseInt(currentUser.id()));
            return statement.executeUpdate() > 0;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to update SQL password");
            return false;
        }
    }

    private boolean updatePasswordInLocalStorage(User currentUser, String validatedPassword) {
        for (int i = 0; i < users.size(); i++) {
            User existing = users.get(i);
            if (!existing.id().equals(currentUser.id())) {
                continue;
            }

            User updated = new User(
                    existing.id(),
                    existing.username(),
                    existing.displayName(),
                    BCrypt.hashpw(validatedPassword, BCrypt.gensalt()),
                    existing.rank(),
                    existing.points(),
                    existing.email(),
                    existing.firstName(),
                    existing.lastName(),
                    existing.phoneNumber(),
                    existing.role(),
                    false
            );
            users.set(i, updated);
            saveToDisk();
            return true;
        }
        return false;
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

    private void loadFromDisk() {
        if (Files.notExists(storageFile)) {
            seedIfEmpty();
            saveToDisk();
            return;
        }

        try {
            String json = Files.readString(storageFile, StandardCharsets.UTF_8);
            List<User> loaded = gson.fromJson(json, USER_LIST_TYPE);
            users.clear();
            users.addAll(loaded == null ? List.of() : loaded);
            if (users.isEmpty()) {
                seedIfEmpty();
                saveToDisk();
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to load users, reseeding defaults");
            users.clear();
            seedIfEmpty();
            saveToDisk();
        }
    }

    private void saveToDisk() {
        try {
            Files.createDirectories(storageFile.getParent());
            String json = gson.toJson(users, USER_LIST_TYPE);
            Files.writeString(
                    storageFile,
                    json,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to save users");
        }
    }

    private void seedIfEmpty() {
        if (!users.isEmpty()) {
            return;
        }

        users.add(new User(
                java.util.UUID.randomUUID().toString(),
                "scribe",
                "Grand Scribe",
                hashPassword("mythoria123"),
                rankFromScore(100),
                100,
                "scribe@mythoria.local",
                "Grand",
                "Scribe",
                "",
                "user",
                false
        ));
    }

    private static User mergeProfile(User user, String email, String firstName, String lastName, String phoneNumber, boolean databaseBacked) {
        String normalizedEmail = ValidationUtils.requireEmail(email);
        String normalizedFirstName = ValidationUtils.optionalName(firstName, "Prenom");
        String normalizedLastName = ValidationUtils.optionalName(lastName, "Nom");
        String normalizedPhone = ValidationUtils.optionalPhone(phoneNumber);
        String displayName = (normalizedFirstName + " " + normalizedLastName).trim();
        if (displayName.isBlank()) {
            displayName = user.displayName();
        }

        return new User(
                user.id(),
                user.username(),
                displayName,
                user.passwordHash(),
                user.rank(),
                user.points(),
                normalizedEmail,
                normalizedFirstName,
                normalizedLastName,
                normalizedPhone,
                user.role(),
                databaseBacked
        );
    }

    public List<User> findAllUsers() {
        String sql = """
                SELECT *
                FROM user
                ORDER BY id DESC
                """;

        List<User> result = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
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

        try (Connection connection = databaseConnection.getConnection()) {
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
        try (Connection connection = databaseConnection.getConnection();
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

        try (Connection connection = databaseConnection.getConnection()) {
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
        String simpleRole = Optional.ofNullable(readStringSafely(rs, "role")).orElse("").trim();
        if (!simpleRole.isBlank()) {
            return normalizeRole(simpleRole);
        }

        String rolesJson = Optional.ofNullable(readStringSafely(rs, "roles")).orElse("").toUpperCase();
        if (rolesJson.contains("ROLE_ADMIN")) {
            return "admin";
        }
        if (rolesJson.contains("ROLE_AUTHOR")) {
            return "author";
        }
        if (rolesJson.contains("ROLE_CLIENT")) {
            return "client";
        }
        if (rolesJson.contains("ROLE_USER")) {
            return "user";
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
