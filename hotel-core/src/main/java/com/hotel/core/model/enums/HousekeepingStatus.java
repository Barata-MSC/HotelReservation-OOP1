package com.hotel.core.model.enums;

public enum HousekeepingStatus {
    CLEAN("Clean"),
    DIRTY("Dirty"),
    UNDER_MAINTENANCE("Under Maintenance"),
    OUT_OF_SERVICE("Out of Service");

    private final String databaseValue;

    HousekeepingStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public static HousekeepingStatus fromDatabaseValue(String value) {
        for (HousekeepingStatus status : values()) {
            if (status.databaseValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown housekeeping status: " + value);
    }
}
