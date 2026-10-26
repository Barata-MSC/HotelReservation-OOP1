package com.hotel.core.model;

import com.hotel.core.model.enums.RoomType;

import java.time.LocalDateTime;

public record CustomerBookingRequest(
        Guest guest,
        int roomNumber,
        RoomType roomType,
        LocalDateTime checkIn,
        LocalDateTime checkOut,
        int guestCount) {
}
