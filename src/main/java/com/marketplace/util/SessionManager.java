package com.marketplace.util;

/**
 * SessionManager — Singleton (same pattern as MyConnection).
 * Stores the currently logged-in user's role and name.
 * Roles: "admin" | "artist" | "client"
 */
public class SessionManager {

    private static SessionManager instance = null;

    private String role; // "admin" | "artist" | "client"
    private String name;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String r) {
        this.role = r;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public boolean isAdmin() {
        return "admin".equals(role);
    }

    public boolean isArtist() {
        return "artist".equals(role);
    }

    public boolean isClient() {
        return "client".equals(role);
    }
}
