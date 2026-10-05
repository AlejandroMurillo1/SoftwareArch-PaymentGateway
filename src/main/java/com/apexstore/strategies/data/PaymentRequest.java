package com.apexstore.strategies.data;

import java.math.BigDecimal;
import java.util.Objects;

public record PaymentRequest(
        int transactionId,
        BigDecimal amount,
        String currency
) {
    public PaymentRequest {
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(currency, "currency is required");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
    }
}
