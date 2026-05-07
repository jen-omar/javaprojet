package tn.esprit.Models;

import java.time.LocalDateTime;

public class Message {
    private int id;
    private int senderId;
    private int briefId;
    private String content;
    private String audioPath;
    private LocalDateTime createdAt;
    
    // For UI display
    private String senderUsername;

    public Message() {}

    public Message(int id, int senderId, int briefId, String content, String audioPath, LocalDateTime createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.briefId = briefId;
        this.content = content;
        this.audioPath = audioPath;
        this.createdAt = createdAt;
    }

    public Message(int senderId, int briefId, String content, String audioPath, LocalDateTime createdAt) {
        this.senderId = senderId;
        this.briefId = briefId;
        this.content = content;
        this.audioPath = audioPath;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }

    public int getBriefId() { return briefId; }
    public void setBriefId(int briefId) { this.briefId = briefId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAudioPath() { return audioPath; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
}
