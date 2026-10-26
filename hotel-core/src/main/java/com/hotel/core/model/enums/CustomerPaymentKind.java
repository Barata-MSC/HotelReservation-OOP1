package com.hotel.core.model.enums;

public enum CustomerPaymentKind {
    DOWNPAYMENT("Downpayment"),
    BALANCE("Balance");

    private final String databaseValue;

    CustomerPaymentKind(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }
}
