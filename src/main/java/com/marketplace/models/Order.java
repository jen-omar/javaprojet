package com.marketplace.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model — maps to the `order` table in the marketplace database.
 * Note: "order" is a SQL reserved word, always quote it with backticks in queries.
 */
public class Order {

    private int id;
    private String buyerName;
    private BigDecimal price;
    private String orderType;      // e.g. purchase
    private LocalDateTime createdAt;
    private int productId;

    // ── Constructors ──────────────────────────────────────────────
    public Order() {}

    public Order(String buyerName, BigDecimal price, String orderType, int productId) {
        this.buyerName  = buyerName;
        this.price      = price;
        this.orderType  = orderType;
        this.productId  = productId;
    }

    // ── Getters & Setters ─────────────────────────────────────────
    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }

    public String getBuyerName()              { return buyerName; }
    public void setBuyerName(String b)        { this.buyerName = b; }

    public BigDecimal getPrice()              { return price; }
    public void setPrice(BigDecimal price)    { this.price = price; }

    public String getOrderType()              { return orderType; }
    public void setOrderType(String ot)       { this.orderType = ot; }

    public LocalDateTime getCreatedAt()       { return createdAt; }
    public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }

    public int getProductId()                 { return productId; }
    public void setProductId(int pid)         { this.productId = pid; }

    @Override
    public String toString() {
        return "Order{id=" + id + ", buyer='" + buyerName + "', price=" + price + "}";
    }
}
