package com.hotel.core.model;

import java.math.BigDecimal;
import java.util.List;

public record AdminReservationDetails(
        CustomerBooking booking,
        BigDecimal verifiedAmount,
        List<AdminPaymentRecord> payments) {

    public AdminReservationDetails {
        payments = List.copyOf(payments);
    }
}
