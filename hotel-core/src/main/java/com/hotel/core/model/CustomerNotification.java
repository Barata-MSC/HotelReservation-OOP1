package com.hotel.core.model;

import java.time.LocalDateTime;

public record CustomerNotification(
        long id,
        String type,
        String message,
        LocalDateTime createdAt,
        boolean read,
        String reservationReference) {
}
