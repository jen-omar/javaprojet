package com.marketplace.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model — maps to the `bid` table in the marketplace database.
 */
public class Bid {

    private int id;
    private String bidderName;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private int productId;

    // ── Constructors ──────────────────────────────────────────────
    public Bid() {
    }

    public Bid(String bidderName, BigDecimal amount, int productId) {
        this.bidderName = bidderName;
        this.amount = amount;
        this.productId = productId;
    }

    // ── Getters & Setters ─────────────────────────────────────────
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBidderName() {
        return bidderName;
    }

    public void setBidderName(String b) {
        this.bidderName = b;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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
        return "Bid{id=" + id + ", bidder='" + bidderName + "', amount=" + amount + "}";
    }
}
