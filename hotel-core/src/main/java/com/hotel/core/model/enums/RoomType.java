package com.hotel.core.model.enums;

public enum RoomType {
    SINGLE("Single"),
    DOUBLE("Double"),
    DELUXE("Deluxe");

    private final String databaseValue;

    RoomType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public static RoomType fromDatabaseValue(String value) {
        for (RoomType type : values()) {
            if (type.databaseValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown room type: " + value);
    }
}
