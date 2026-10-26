package com.hotel.core.model;

import com.hotel.core.model.enums.CustomerPaymentStatus;

import java.math.BigDecimal;

public record CustomerReceipt(
        CustomerBooking booking,
        CheckoutBill bill,
        BigDecimal amountPaid,
        BigDecimal remainingBalance,
        CustomerPaymentStatus paymentStatus,
        String paymentMethod) {
}
