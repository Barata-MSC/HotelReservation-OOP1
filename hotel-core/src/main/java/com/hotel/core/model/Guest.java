package com.hotel.core.model;

import com.hotel.core.model.enums.GuestClassification;

import java.time.LocalDate;

public record Guest(
        long id,
        String fullName,
        String contactNumber,
        String email,
        GuestClassification classification,
        String discountIdType,
        String discountIdNumber,
        boolean discountVerified,
        LocalDate discountIdExpiry,
        boolean discountNameMatches) {

    public Guest(
            long id,
            String fullName,
            String contactNumber,
            String email,
            GuestClassification classification,
            String discountIdType,
            String discountIdNumber,
            boolean discountVerified) {
        this(id, fullName, contactNumber, email, classification, discountIdType,
                discountIdNumber, discountVerified, null, false);
    }
}
