package com.example.mythoriadesktop.model;

import java.time.LocalDateTime;
import java.util.Optional;

public record ChatMessage(
        String sender,
        String message,
        LocalDateTime createdAt
) {
    public ChatMessage {
        sender = Optional.ofNullable(sender).orElse("AI").trim();
        message = Optional.ofNullable(message).orElse("").trim();
        createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public boolean fromUser() {
        return "USER".equalsIgnoreCase(sender);
    }
}
