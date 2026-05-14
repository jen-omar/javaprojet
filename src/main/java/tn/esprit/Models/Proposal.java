package tn.esprit.Models;

import java.time.LocalDateTime;

public class Proposal {
    private int id;
    private double price;
    private int daysToComplete;
    private String coverLetter;
    private LocalDateTime submittedAt;
    private boolean isAccepted;
    private int artistId;
    private String artistUsername;
    private int briefId;

    // Constructors
    public Proposal() {}

    public Proposal(int id, double price, int daysToComplete, String coverLetter, LocalDateTime submittedAt, boolean isAccepted, int artistId, int briefId) {
        this.id = id;
        this.price = price;
        this.daysToComplete = daysToComplete;
        this.coverLetter = coverLetter;
        this.submittedAt = submittedAt;
        this.isAccepted = isAccepted;
        this.artistId = artistId;
        this.briefId = briefId;
    }

    public Proposal(double price, int daysToComplete, String coverLetter, LocalDateTime submittedAt, boolean isAccepted, int artistId, int briefId) {
        this.price = price;
        this.daysToComplete = daysToComplete;
        this.coverLetter = coverLetter;
        this.submittedAt = submittedAt;
        this.isAccepted = isAccepted;
        this.artistId = artistId;
        this.briefId = briefId;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    
    public int getDaysToComplete() { return daysToComplete; }
    public void setDaysToComplete(int daysToComplete) { this.daysToComplete = daysToComplete; }
    
    public String getCoverLetter() { return coverLetter; }
    public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }
    
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    
    public boolean isAccepted() { return isAccepted; }
    public void setAccepted(boolean accepted) { isAccepted = accepted; }
    
    public int getArtistId() { return artistId; }
    public void setArtistId(int artistId) { this.artistId = artistId; }
    
    public String getArtistUsername() { return artistUsername; }
    public void setArtistUsername(String artistUsername) { this.artistUsername = artistUsername; }

    public int getBriefId() { return briefId; }
    public void setBriefId(int briefId) { this.briefId = briefId; }
}
