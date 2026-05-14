package com.marketplace.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model — maps to the `product` table in the marketplace database.
 */
public class Product {

    private int id;
    private String name;
    private String description;
    private BigDecimal price;
    private String artistName;
    private LocalDateTime createdAt;
    private String imageUrl;
    private String type;       // e.g. Digital Art, Painting, Sculpture
    private String category;   // e.g. Landscape, Portrait, Modern
    private String saleType;   // fixed | auction
    private String status;     // available | sold | annulee
    private LocalDateTime auctionEndTime;
    private BigDecimal currentBid;
    private String currentBidder;
    private String buyer;
    private BigDecimal reservePrice;
    private BigDecimal minBidIncrement;
    private LocalDateTime auctionStartTime;

    // ── Constructors ──────────────────────────────────────────────
    public Product() {}

    public Product(String name, String description, BigDecimal price,
                   String artistName, String imageUrl, String type,
                   String category, String saleType, String status) {
        this.name        = name;
        this.description = description;
        this.price       = price;
        this.artistName  = artistName;
        this.imageUrl    = imageUrl;
        this.type        = type;
        this.category    = category;
        this.saleType    = saleType;
        this.status      = status;
    }

    // ── Getters & Setters ─────────────────────────────────────────
    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }

    public String getName()                   { return name; }
    public void setName(String name)          { this.name = name; }

    public String getDescription()            { return description; }
    public void setDescription(String d)      { this.description = d; }

    public BigDecimal getPrice()              { return price; }
    public void setPrice(BigDecimal price)    { this.price = price; }

    public String getArtistName()             { return artistName; }
    public void setArtistName(String a)       { this.artistName = a; }

    public LocalDateTime getCreatedAt()       { return createdAt; }
    public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }

    public String getImageUrl()               { return imageUrl; }
    public void setImageUrl(String url)       { this.imageUrl = url; }

    public String getType()                   { return type; }
    public void setType(String type)          { this.type = type; }

    public String getCategory()               { return category; }
    public void setCategory(String c)         { this.category = c; }

    public String getSaleType()               { return saleType; }
    public void setSaleType(String st)        { this.saleType = st; }

    public String getStatus()                 { return status; }
    public void setStatus(String status)      { this.status = status; }

    public LocalDateTime getAuctionEndTime()       { return auctionEndTime; }
    public void setAuctionEndTime(LocalDateTime t)  { this.auctionEndTime = t; }

    public BigDecimal getCurrentBid()              { return currentBid; }
    public void setCurrentBid(BigDecimal cb)       { this.currentBid = cb; }

    public String getCurrentBidder()               { return currentBidder; }
    public void setCurrentBidder(String cb)        { this.currentBidder = cb; }

    public String getBuyer()                       { return buyer; }
    public void setBuyer(String buyer)             { this.buyer = buyer; }

    public BigDecimal getReservePrice()            { return reservePrice; }
    public void setReservePrice(BigDecimal rp)     { this.reservePrice = rp; }

    public BigDecimal getMinBidIncrement()         { return minBidIncrement; }
    public void setMinBidIncrement(BigDecimal mbi) { this.minBidIncrement = mbi; }

    public LocalDateTime getAuctionStartTime()      { return auctionStartTime; }
    public void setAuctionStartTime(LocalDateTime t){ this.auctionStartTime = t; }

    @Override
    public String toString() {
        return "Product{id=" + id + ", name='" + name + "', price=" + price + ", status='" + status + "'}";
    }
}
