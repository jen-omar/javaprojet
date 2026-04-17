package tn.esprit.Models;

import java.util.ArrayList;
import java.util.List;

public class User {
    private int id;
    private String email;
    private String username;
    private String password;
    private boolean isBotEnabled;
    private String roles; // stores JSON string like ["ROLE_ADMIN"]

    public User() {}

    public User(int id, String email, String username, String password, boolean isBotEnabled, String roles) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.isBotEnabled = isBotEnabled;
        this.roles = roles;
    }

    public User(String email, String username, String password, boolean isBotEnabled, String roles) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.isBotEnabled = isBotEnabled;
        this.roles = roles;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isBotEnabled() { return isBotEnabled; }
    public void setBotEnabled(boolean botEnabled) { isBotEnabled = botEnabled; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    // Helper to get first role (used for basic routing)
    public String getPrimaryRole() {
        if (roles == null || roles.isEmpty()) return "ROLE_USER";
        if (roles.contains("ROLE_ADMIN")) return "ROLE_ADMIN";
        if (roles.contains("ROLE_AUTHOR")) return "ROLE_AUTHOR";
        if (roles.contains("ROLE_CLIENT")) return "ROLE_CLIENT";
        return "ROLE_USER";
    }
}
