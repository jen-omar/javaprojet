package tn.esprit.mythoria.entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Event {

    private int id;
    private String title;
    private String description;
    private String image;
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

    public Event(int id, String title, String description, String image, LocalDateTime date, String location,
                 LocalDateTime createdAt, int maxTickets, int maxVipTickets,
                 int maxNormalTickets, int creatorId, int localId) {
        this(id, title, description, date, location, createdAt, maxTickets, maxVipTickets, maxNormalTickets, creatorId, localId);
        this.image = image;
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

    public Event(String title, String description, String image, LocalDateTime date, String location,
                 LocalDateTime createdAt, int maxTickets, int maxVipTickets,
                 int maxNormalTickets, int creatorId, int localId) {
        this(title, description, date, location, createdAt, maxTickets, maxVipTickets, maxNormalTickets, creatorId, localId);
        this.image = image;
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String displayDate = date != null ? date.format(formatter) : "Date N/A";
        String displayTitle = title != null && !title.isBlank() ? title : "Sans titre";
        String displayLocation = location != null && !location.isBlank() ? location : "Lieu N/A";

        return displayTitle + " | " +
                displayDate + " | " +
                displayLocation + " | " +
                "Tickets: " + maxTickets +
                " (VIP: " + maxVipTickets + ", Normal: " + maxNormalTickets + ")";
    }
}
