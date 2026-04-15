package com.marketplace.models;

import java.time.LocalDateTime;

/**
 * Model — maps to the `review` table in the marketplace database.
 */
public class Review {

    private int id;
    private String reviewerName;
    private int rating; // 1–5
    private String comment;
    private LocalDateTime createdAt;
    private int productId;

    // ── Constructors ──────────────────────────────────────────────
    public Review() {
    }

    public Review(String reviewerName, int rating, String comment, int productId) {
        this.reviewerName = reviewerName;
        this.rating = rating;
        this.comment = comment;
        this.productId = productId;
    }

    // ── Getters & Setters ─────────────────────────────────────────
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String r) {
        this.reviewerName = r;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String c) {
        this.comment = c;
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

    /** Returns a star string like "★★★☆☆" based on rating 1–5. */
    public String getStars() {
        return "★".repeat(Math.max(0, Math.min(rating, 5))) +
                "☆".repeat(Math.max(0, 5 - Math.min(rating, 5)));
    }

    @Override
    public String toString() {
        return "Review{id=" + id + ", reviewer='" + reviewerName + "', rating=" + rating + "}";
    }
}
