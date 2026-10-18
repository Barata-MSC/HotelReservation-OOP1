package com.hotel.core.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CheckoutReceipt(
        String receiptReference,
        Reservation reservation,
        CheckoutBill bill,
        BigDecimal cashTendered,
        BigDecimal change,
        LocalDateTime paidAt) {
}
