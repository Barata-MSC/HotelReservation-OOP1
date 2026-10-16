package com.hotel.core.model;

import java.util.Objects;

/** Stored staff account used only by the core authentication and repository layers. */
public final class StoredStaffAccount {

    private final int id;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final String role;
    private final boolean enabled;

    public StoredStaffAccount(
            int id, String username, String passwordHash, String fullName, String role, boolean enabled) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "username");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.fullName = Objects.requireNonNull(fullName, "fullName");
        this.role = Objects.requireNonNull(role, "role");
        this.enabled = enabled;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public StaffUser toStaffUser() {
        return new StaffUser(id, username, fullName, role);
    }
}
