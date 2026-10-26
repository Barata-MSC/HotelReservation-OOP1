package com.hotel.core.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalkInReceipt(
        Reservation reservation,
        CheckoutBill bill,
        String receiptReference,
        BigDecimal cashTendered,
        BigDecimal change,
        LocalDateTime paidAt) {
}
