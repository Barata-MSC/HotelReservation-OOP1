package com.hotel.core.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StayExtensionQuote(
        String reference,
        LocalDateTime currentCheckout,
        LocalDateTime proposedCheckout,
        int currentNights,
        int proposedNights,
        BigDecimal currentTotal,
        BigDecimal proposedTotal,
        BigDecimal extensionCharge) {
}
