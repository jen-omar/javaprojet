package tn.esprit.Models;

import java.time.LocalDateTime;

public class Brief {
    private int id;
    private String title;
    private String description;
    private double budgetMax;
    private LocalDateTime deadline;
    private String status;
    private int clientId;
    private String clientUsername;
    private LocalDateTime createdAt;

    // Constructors
    public Brief() {}

    public Brief(int id, String title, String description, double budgetMax, LocalDateTime deadline, String status, int clientId, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.budgetMax = budgetMax;
        this.deadline = deadline;
        this.status = status;
        this.clientId = clientId;
        this.createdAt = createdAt;
    }

    public Brief(String title, String description, double budgetMax, LocalDateTime deadline, String status, int clientId, LocalDateTime createdAt) {
        this.title = title;
        this.description = description;
        this.budgetMax = budgetMax;
        this.deadline = deadline;
        this.status = status;
        this.clientId = clientId;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public double getBudgetMax() { return budgetMax; }
    public void setBudgetMax(double budgetMax) { this.budgetMax = budgetMax; }
    
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }
    
    public String getClientUsername() { return clientUsername; }
    public void setClientUsername(String clientUsername) { this.clientUsername = clientUsername; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
