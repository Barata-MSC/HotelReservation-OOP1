package com.hotel.core.model.enums;

public enum CustomerPaymentStatus {
    UNPAID("Unpaid"),
    PARTIALLY_PAID("Partially Paid"),
    FULLY_PAID("Fully Paid");

    private final String databaseValue;

    CustomerPaymentStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public static CustomerPaymentStatus fromDatabaseValue(String value) {
        for (CustomerPaymentStatus status : values()) {
            if (status.databaseValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown payment status: " + value);
    }
}
