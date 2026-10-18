package com.hotel.core.model;

import com.hotel.core.model.enums.ReservationStatus;
import com.hotel.core.model.enums.ReservationType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Reservation(
        long id,
        String reference,
        Guest guest,
        int roomNumber,
        String roomType,
        BigDecimal roomRate,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        int guestCount,
        ReservationType type,
        ReservationStatus status) {
}
