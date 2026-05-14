package tn.esprit.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public final class DatabaseConfig {
    private static final Properties PROPERTIES = loadProperties();

    private DatabaseConfig() {
    }

    public static String url() {
        return read("DB_URL", "db.url")
                .orElse("jdbc:mysql://localhost:3306/mythoria_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
    }

    public static String user() {
        return read("DB_USER", "db.user").orElse("root");
    }

    public static String password() {
        return read("DB_PASSWORD", "db.password").orElse("");
    }

    private static Optional<String> read(String envKey, String propertyKey) {
        String systemValue = System.getProperty(propertyKey);
        if (systemValue != null && !systemValue.isBlank()) {
            return Optional.of(systemValue.trim());
        }

        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return Optional.of(envValue.trim());
        }

        String propertyValue = PROPERTIES.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return Optional.of(propertyValue.trim());
        }

        return Optional.empty();
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConfig.class.getResourceAsStream("/config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
            // Defaults keep local development working even when config.properties is absent.
        }
        return properties;
    }
}
