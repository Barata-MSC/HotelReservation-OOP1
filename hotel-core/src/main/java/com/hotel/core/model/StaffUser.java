package com.hotel.core.model;

import java.util.Objects;

/** Authenticated staff profile; contains no credential material. */
public final class StaffUser {

    private final int id;
    private final String username;
    private final String fullName;
    private final String role;

    public StaffUser(int id, String username, String fullName, String role) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "username");
        this.fullName = Objects.requireNonNull(fullName, "fullName");
        this.role = Objects.requireNonNull(role, "role");
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }
}
