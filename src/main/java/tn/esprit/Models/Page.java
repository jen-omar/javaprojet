package tn.esprit.Models;

import java.time.Instant;
import java.util.UUID;

public record Page(UUID id, UUID bookId, String type, String content, Instant createdAt) {
    public static Page createNew(UUID bookId, String type, String content) {
        return new Page(UUID.randomUUID(), bookId, safe(type), safe(content), Instant.now());
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
