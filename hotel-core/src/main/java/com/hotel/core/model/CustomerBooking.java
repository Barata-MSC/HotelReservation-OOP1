package com.hotel.core.model;

import com.hotel.core.model.enums.CustomerPaymentStatus;

import java.math.BigDecimal;
import java.time.LocalTime;

public record CustomerBooking(
        Reservation reservation,
        LocalTime checkInTime,
        LocalTime checkOutTime,
        CustomerPaymentStatus paymentStatus,
        BigDecimal total,
        BigDecimal amountSubmitted,
        BigDecimal balance,
        String paymentMethods) {
}
