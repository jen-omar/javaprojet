package tn.esprit.controllers.services;

import tn.esprit.Models.User;
import tn.esprit.util.MyConnection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public final class OtpService {
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Integer> attemptsByUserId = new ConcurrentHashMap<>();
    private final int expirationMinutes;

    public OtpService() {
        this.expirationMinutes = readExpirationMinutes();
    }

    public String generateAndSaveOtp(User user) {
        requireDatabaseUser(user);
        String otpCode = String.format("%06d", RANDOM.nextInt(1_000_000));

        String markOldCodesSql = """
                UPDATE otp_codes
                SET is_used = TRUE
                WHERE user_id = ? AND is_used = FALSE
                """;
        String insertSql = """
                INSERT INTO otp_codes (user_id, otp_code, expiration_time, is_used)
                VALUES (?, ?, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL ? MINUTE), FALSE)
                """;

        try (Connection connection = MyConnection.getInstance().getConnection()) {
            ensureOtpTable(connection);
            try (PreparedStatement markOldCodes = connection.prepareStatement(markOldCodesSql);
                 PreparedStatement insert = connection.prepareStatement(insertSql)) {
                markOldCodes.setInt(1, Integer.parseInt(user.id()));
                markOldCodes.executeUpdate();

                insert.setInt(1, Integer.parseInt(user.id()));
                insert.setString(2, otpCode);
                insert.setInt(3, expirationMinutes);
                insert.executeUpdate();
            }
            attemptsByUserId.remove(user.id());
            return otpCode;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to save OTP code.", ex);
        }
    }

    public boolean verifyOtp(User user, String submittedCode) {
        requireDatabaseUser(user);
        String code = Optional.ofNullable(submittedCode).orElse("").trim();
        if (!code.matches("\\d{6}")) {
            registerFailedAttempt(user);
            return false;
        }
        if (attemptsByUserId.getOrDefault(user.id(), 0) >= MAX_ATTEMPTS) {
            return false;
        }

        String markUsedSql = """
                UPDATE otp_codes
                SET is_used = TRUE
                WHERE user_id = ?
                  AND otp_code = ?
                  AND is_used = FALSE
                  AND expiration_time > CURRENT_TIMESTAMP
                """;

        try (Connection connection = MyConnection.getInstance().getConnection()) {
            ensureOtpTable(connection);
            try (PreparedStatement markUsed = connection.prepareStatement(markUsedSql)) {
                markUsed.setInt(1, Integer.parseInt(user.id()));
                markUsed.setString(2, code);
                if (markUsed.executeUpdate() == 0) {
                    registerFailedAttempt(user);
                    return false;
                }
                attemptsByUserId.remove(user.id());
                return true;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to verify OTP code.", ex);
        }
    }

    public int expirationMinutes() {
        return expirationMinutes;
    }

    private void registerFailedAttempt(User user) {
        attemptsByUserId.merge(user.id(), 1, Integer::sum);
    }

    private static void requireDatabaseUser(User user) {
        if (user == null || !user.databaseBacked()) {
            throw new IllegalArgumentException("OTP requires a MySQL user.");
        }
    }

    private static void ensureOtpTable(Connection connection) throws Exception {
        String createTableSql = """
                CREATE TABLE IF NOT EXISTS otp_codes (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    otp_code VARCHAR(10) NOT NULL,
                    expiration_time TIMESTAMP NOT NULL,
                    is_used BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_otp_codes_user_id (user_id),
                    INDEX idx_otp_codes_expiration_time (expiration_time)
                )
                """;
        String migrateExpirationColumnSql = """
                ALTER TABLE otp_codes
                MODIFY COLUMN expiration_time TIMESTAMP NOT NULL
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSql);
            statement.execute(migrateExpirationColumnSql);
        }
    }

    private static int readExpirationMinutes() {
        Properties properties = loadProperties();
        String value = Optional.ofNullable(System.getenv("OTP_EXPIRATION_MINUTES"))
                .filter(v -> !v.isBlank())
                .orElseGet(() -> Optional.ofNullable(properties.getProperty("otp.expiration.minutes")).orElse("5"))
                .trim();
        try {
            int minutes = Integer.parseInt(value);
            return minutes > 0 ? minutes : 5;
        } catch (NumberFormatException ignored) {
            return 5;
        }
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
        InputStream classpathStream = OtpService.class.getClassLoader().getResourceAsStream("config.properties");
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
