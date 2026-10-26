package com.hotel.core.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record BookingHistoryEntry(
        long reservationId,
        String reference,
        String guestName,
        int roomNumber,
        LocalDate checkIn,
        LocalTime checkInTime,
        LocalDate checkOut,
        LocalTime checkOutTime,
        int guestCount,
        String reservationType,
        String reservationStatus,
        String eventType,
        String eventNote,
        LocalDateTime eventAt) {
}
