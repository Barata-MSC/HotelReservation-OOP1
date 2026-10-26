package com.hotel.core.model;

import java.util.Objects;

public final class AuthenticatedUser {

    private final int id;
    private final String username;
    private final String email;
    private final String contactNumber;
    private final String fullName;
    private final UserRole role;

    public AuthenticatedUser(
            int id,
            String username,
            String email,
            String contactNumber,
            String fullName,
            UserRole role) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "username");
        this.email = email;
        this.contactNumber = contactNumber;
        this.fullName = Objects.requireNonNull(fullName, "fullName");
        this.role = Objects.requireNonNull(role, "role");
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public UserRole getRole() {
        return role;
    }
}
