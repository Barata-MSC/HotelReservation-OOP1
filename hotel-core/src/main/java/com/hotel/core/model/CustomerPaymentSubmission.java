package com.hotel.core.model;

import com.hotel.core.model.enums.CustomerPaymentMethod;

import java.math.BigDecimal;
import java.nio.file.Path;

public record CustomerPaymentSubmission(
        CustomerPaymentMethod method,
        BigDecimal amount,
        String referenceDetails,
        Path proofFile) {
}
