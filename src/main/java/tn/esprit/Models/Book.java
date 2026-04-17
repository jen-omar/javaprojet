package tn.esprit.Models;

import java.time.Instant;
import java.util.UUID;

public record Book(UUID id, UUID worldId, String title, String description, Instant createdAt) {
    public static Book createNew(UUID worldId, String title, String description) {
        return new Book(UUID.randomUUID(), worldId, safe(title), safe(description), Instant.now());
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
