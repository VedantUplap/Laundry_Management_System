package com.lsms.models;

import java.sql.Timestamp;

public class User {
    private int userId;
    private String username;
    private String email;
    private String role;
    private boolean isActive;
    private Timestamp createdAt;

    public User() {}

    public User(int userId, String username, String email, String role, boolean isActive) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.isActive = isActive;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("[%d] %-15s | %-28s | Role: %-8s | Active: %s",
                userId, username, email, role, isActive ? "Yes" : "No");
    }
}
