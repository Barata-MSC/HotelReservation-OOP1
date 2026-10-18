package com.hotel.core.model.enums;

public enum GuestClassification {
    REGULAR("Regular"),
    DISCOUNTED("Discounted");

    private final String databaseValue;

    GuestClassification(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public static GuestClassification fromDatabaseValue(String value) {
        for (GuestClassification classification : values()) {
            if (classification.databaseValue.equals(value)) {
                return classification;
            }
        }
        throw new IllegalArgumentException("Unknown guest classification: " + value);
    }
}
