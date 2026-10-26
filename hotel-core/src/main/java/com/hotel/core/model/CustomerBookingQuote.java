package com.hotel.core.model;

import java.math.BigDecimal;

public record CustomerBookingQuote(
        CheckoutBill bill,
        BigDecimal downpayment,
        BigDecimal remainingBalance,
        boolean discountIsProvisional) {
}
