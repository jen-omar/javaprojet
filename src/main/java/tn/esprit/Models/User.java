package tn.esprit.Models;

import java.util.Optional;
import java.util.UUID;

public record User(
        String id,
        String username,
        String displayName,
        String passwordHash,
        String rank,
        int points,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String role,
        boolean databaseBacked
) {
    public User(String id, String username, String displayName, String passwordHash, String rank, int points) {
        this(id, username, displayName, passwordHash, rank, points, "", "", "", "", "user", false);
    }

    public User {
        id = Optional.ofNullable(id).orElse(UUID.randomUUID().toString()).trim();
        username = normalize(username);
        displayName = Optional.ofNullable(displayName).orElse("").trim();
        passwordHash = Optional.ofNullable(passwordHash).orElse("").trim();
        rank = Optional.ofNullable(rank).orElse("Neophyte").trim();
        email = Optional.ofNullable(email).orElse("").trim();
        firstName = Optional.ofNullable(firstName).orElse("").trim();
        lastName = Optional.ofNullable(lastName).orElse("").trim();
        phoneNumber = Optional.ofNullable(phoneNumber).orElse("").trim();
        role = normalizeRole(role);
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    private static String normalize(String value) {
        return Optional.ofNullable(value).orElse("").trim().toLowerCase();
    }

    private static String normalizeRole(String value) {
        String normalized = Optional.ofNullable(value).orElse("user").trim().toLowerCase();
        return normalized.isBlank() ? "user" : normalized;
    }
}
