package com.hotel.core.model;

import java.math.BigDecimal;

public record CheckoutBill(
        int nights,
        BigDecimal roomRate,
        BigDecimal roomCost,
        BigDecimal discountRate,
        BigDecimal discountAmount,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal total) {
}
