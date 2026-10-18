package com.hotel.core.model.enums;

public enum ReservationStatus {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    CHECKED_IN("Checked-In"),
    CHECKED_OUT("Checked-Out"),
    CANCELLED("Cancelled");

    private final String databaseValue;

    ReservationStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public static ReservationStatus fromDatabaseValue(String value) {
        for (ReservationStatus status : values()) {
            if (status.databaseValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown reservation status: " + value);
    }
}
