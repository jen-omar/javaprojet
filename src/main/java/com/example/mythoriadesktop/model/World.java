package com.example.mythoriadesktop.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record World(UUID id, String title, String description, String loreSnapshot, Instant createdAt, List<Book> books) {
    public static World createNew(String title, String description, String loreSnapshot) {
        return new World(UUID.randomUUID(), safe(title), safe(description), safe(loreSnapshot), Instant.now(), new ArrayList<>());
    }

    public World withTitle(String title) {
        return new World(id, safe(title), description, loreSnapshot, createdAt, books);
    }

    public World withDescription(String description) {
        return new World(id, title, safe(description), loreSnapshot, createdAt, books);
    }

    public World withLoreSnapshot(String loreSnapshot) {
        return new World(id, title, description, safe(loreSnapshot), createdAt, books);
    }

    public World withBooks(List<Book> newBooks) {
        return new World(id, title, description, loreSnapshot, createdAt, newBooks == null ? new ArrayList<>() : newBooks);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}

