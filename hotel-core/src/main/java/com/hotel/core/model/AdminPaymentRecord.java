package com.hotel.core.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminPaymentRecord(
        long id,
        String type,
        String method,
        BigDecimal amount,
        String referenceDetails,
        String proofFilePath,
        boolean verified,
        LocalDateTime submittedAt) {
}
