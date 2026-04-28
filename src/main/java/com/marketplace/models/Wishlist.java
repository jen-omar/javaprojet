package com.marketplace.models;

import java.time.LocalDateTime;

/**
 * Model — maps to the `wishlist` table in the marketplace database.
 */
public class Wishlist {

    private int id;
    private String clientName;
    private LocalDateTime createdAt;
    private int productId;

    // ── Constructors ──────────────────────────────────────────────
    public Wishlist() {
    }

    public Wishlist(String clientName, int productId) {
        this.clientName = clientName;
        this.productId = productId;
    }

    // ── Getters & Setters ─────────────────────────────────────────
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String c) {
        this.clientName = c;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime c) {
        this.createdAt = c;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int pid) {
        this.productId = pid;
    }

    @Override
    public String toString() {
        return "Wishlist{id=" + id + ", client='" + clientName + "', productId=" + productId + "}";
    }
}
