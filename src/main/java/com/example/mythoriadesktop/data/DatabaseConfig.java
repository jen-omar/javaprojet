package com.example.mythoriadesktop.data;

import java.util.Optional;

public record DatabaseConfig(
        String url,
        String user,
        String password
) {
    private static final String DEFAULT_DB_NAME = "mythoria";
    private static final String DEFAULT_DB_USER = "root";

    public static DatabaseConfig fromEnvironment() {
        String url = read("MYTHORIA_DB_URL", "db.url");
        String user = read("MYTHORIA_DB_USER", "db.user");
        String password = read("MYTHORIA_DB_PASSWORD", "db.password");
        String databaseName = read("MYTHORIA_DB_NAME", "db.name");

        if (databaseName.isBlank()) {
            databaseName = DEFAULT_DB_NAME;
        }

        if (url.isBlank()) {
            url = defaultMysqlUrl(databaseName);
        }

        if (user.isBlank()) {
            user = DEFAULT_DB_USER;
        }

        return new DatabaseConfig(
                url,
                user,
                password
        );
    }

    public boolean isConfigured() {
        return !url.isBlank() && !user.isBlank();
    }

    private static String read(String envKey, String propertyKey) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        return Optional.ofNullable(System.getProperty(propertyKey)).orElse("").trim();
    }

    private static String defaultMysqlUrl(String databaseName) {
        return "jdbc:mysql://localhost:3306/" + databaseName
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }
}
