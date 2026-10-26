package com.hotel.core.model;

import java.util.List;

public record AdminReservationData(
        CustomerBookingData booking,
        List<AdminPaymentRecord> payments) {

    public AdminReservationData {
        payments = List.copyOf(payments);
    }
}
