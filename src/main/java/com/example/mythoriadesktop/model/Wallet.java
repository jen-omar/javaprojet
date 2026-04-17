package com.example.mythoriadesktop.model;

import java.util.Optional;

public record Wallet(
        int id,
        String updatedAt,
        int userId,
        double balance,
        String status,
        String currency,
        double ceiling
) {
    public Wallet {
        updatedAt = Optional.ofNullable(updatedAt).orElse("").trim();
        status = Optional.ofNullable(status).orElse("").trim();
        currency = Optional.ofNullable(currency).orElse("").trim().toUpperCase();
    }
}
