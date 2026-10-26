package com.hotel.core.model.enums;

public enum CustomerPaymentMethod {
    GCASH("GCash", "GCash"),
    BANK_TRANSFER("Bank Transfer", "Bank Transfer"),
    OTHER("Other", "Card / Other");

    private final String databaseValue;
    private final String displayName;

    CustomerPaymentMethod(String databaseValue, String displayName) {
        this.databaseValue = databaseValue;
        this.displayName = displayName;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public String getDisplayName() {
        return displayName;
    }
}
