package com.hotel.core.model;

public enum UserRole {
    ADMIN("Admin"),
    CUSTOMER("Customer");

    private final String databaseValue;

    UserRole(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public static UserRole fromDatabaseValue(String value) {
        for (UserRole role : values()) {
            if (role.databaseValue.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unsupported user role");
    }

    public boolean canAccessAdminWorkspace() {
        return this == ADMIN;
    }
}
