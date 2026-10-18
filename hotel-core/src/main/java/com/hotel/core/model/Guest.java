package com.hotel.core.model;

import com.hotel.core.model.enums.GuestClassification;

public record Guest(
        long id,
        String fullName,
        String contactNumber,
        String email,
        GuestClassification classification,
        String discountIdType,
        String discountIdNumber,
        boolean discountVerified) {
}
