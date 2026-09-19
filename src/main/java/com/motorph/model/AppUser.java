package com.motorph.model;

/**
 * Plain data holder for a row from the app_user table.
 * Kept deliberately simple for the IAS 2 MVP — one user, one role.
 */
public class AppUser {

    private final String username;
    private final String passwordHash; // BCrypt hash, never plaintext
    private final String role;         // "ADMIN" or "EMPLOYEE"

    public AppUser(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }
}
