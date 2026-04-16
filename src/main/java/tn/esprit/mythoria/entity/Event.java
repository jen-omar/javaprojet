package tn.esprit.mythoria.entity;

import java.time.LocalDateTime;

public class Event {

    private int id;
    private String title;
    private String description;
    private LocalDateTime date;
    private String location;
    private LocalDateTime createdAt;
    private int maxTickets;
    private int maxVipTickets;
    private int maxNormalTickets;
    private int creatorId;
    private int localId;

    public Event() {
    }

    public Event(int id, String title, String description, LocalDateTime date, String location,
                 LocalDateTime createdAt, int maxTickets, int maxVipTickets,
                 int maxNormalTickets, int creatorId, int localId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.location = location;
        this.createdAt = createdAt;
        this.maxTickets = maxTickets;
        this.maxVipTickets = maxVipTickets;
        this.maxNormalTickets = maxNormalTickets;
        this.creatorId = creatorId;
        this.localId = localId;
    }

    public Event(String title, String description, LocalDateTime date, String location,
                 LocalDateTime createdAt, int maxTickets, int maxVipTickets,
                 int maxNormalTickets, int creatorId, int localId) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.location = location;
        this.createdAt = createdAt;
        this.maxTickets = maxTickets;
        this.maxVipTickets = maxVipTickets;
        this.maxNormalTickets = maxNormalTickets;
        this.creatorId = creatorId;
        this.localId = localId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }



    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }



    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }



    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }



    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }



    public int getMaxTickets() {
        return maxTickets;
    }

    public void setMaxTickets(int maxTickets) {
        this.maxTickets = maxTickets;
    }



    public int getMaxVipTickets() {
        return maxVipTickets;
    }

    public void setMaxVipTickets(int maxVipTickets) {
        this.maxVipTickets = maxVipTickets;
    }



    public int getMaxNormalTickets() {
        return maxNormalTickets;
    }

    public void setMaxNormalTickets(int maxNormalTickets) {
        this.maxNormalTickets = maxNormalTickets;
    }



    public int getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
    }



    public int getLocalId() {
        return localId;
    }

    public void setLocalId(int localId) {
        this.localId = localId;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", date=" + date +
                ", location='" + location + '\'' +
                ", createdAt=" + createdAt +
                ", maxTickets=" + maxTickets +
                ", maxVipTickets=" + maxVipTickets +
                ", maxNormalTickets=" + maxNormalTickets +
                ", creatorId=" + creatorId +
                ", localId=" + localId +
                '}';
    }
}
