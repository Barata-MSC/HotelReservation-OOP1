package com.hotel.core.model;

import java.util.Objects;

public final class StoredUserAccount {

    private final int id;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final String email;
    private final String contactNumber;
    private final UserRole role;
    private final boolean enabled;

    public StoredUserAccount(
            int id,
            String username,
            String passwordHash,
            String fullName,
            String email,
            String contactNumber,
            UserRole role,
            boolean enabled) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "username");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.fullName = Objects.requireNonNull(fullName, "fullName");
        this.email = email;
        this.contactNumber = contactNumber;
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

    public String getEmail() {
        return email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public AuthenticatedUser toAuthenticatedUser() {
        return new AuthenticatedUser(id, username, email, contactNumber, fullName, role);
    }

    public StoredUserAccount withId(int generatedId) {
        return new StoredUserAccount(
                generatedId, username, passwordHash, fullName, email, contactNumber, role, enabled);
    }
}
