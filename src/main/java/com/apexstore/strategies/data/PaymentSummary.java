package com.apexstore.strategies.data;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record PaymentSummary(
        int transactionId,
        PaymentStatus paymentStatus,
        UUID externalRef,
        Optional<String> failureCause
) {
    public PaymentSummary {
        Objects.requireNonNull(paymentStatus, "paymentStatus is required");
        Objects.requireNonNull(externalRef, "externalRef is required");
        Objects.requireNonNull(failureCause, "failureCause must be Optional.empty(), not null");

        if (paymentStatus == PaymentStatus.CONFIRMED && failureCause.isPresent()) {
            throw new IllegalArgumentException("A CONFIRMED summary must not have a failureCause");
        }
        if (paymentStatus == PaymentStatus.FAILED && failureCause.isEmpty()) {
            throw new IllegalArgumentException("A FAILED summary requires a failureCause");
        }
    }
}
