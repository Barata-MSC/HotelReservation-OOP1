package com.hotel.core.model;

import com.hotel.core.model.enums.CustomerPaymentStatus;

import java.math.BigDecimal;
import java.time.LocalTime;

public record CustomerBookingData(
        Reservation reservation,
        LocalTime checkInTime,
        LocalTime checkOutTime,
        CustomerPaymentStatus paymentStatus,
        BigDecimal amountSubmitted,
        String paymentMethods) {
}
