package com.hotel.core.model.enums;

public enum ReservationType {
    ONLINE("Online"),
    WALK_IN("Walk-in");

    private final String databaseValue;

    ReservationType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public static ReservationType fromDatabaseValue(String value) {
        for (ReservationType type : values()) {
            if (type.databaseValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown reservation type: " + value);
    }
}
