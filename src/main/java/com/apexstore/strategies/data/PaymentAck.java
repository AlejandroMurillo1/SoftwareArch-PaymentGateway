package com.apexstore.strategies.data;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record PaymentAck(
        AckStatus status,
        Optional<UUID> externalRef,
        Optional<String> rejectedCause
) {
    public PaymentAck {
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(externalRef, "externalRef must be Optional.empty(), not null");
        Objects.requireNonNull(rejectedCause, "rejectedCause must be Optional.empty(), not null");

        if (status == AckStatus.ACCEPTED && (externalRef.isEmpty() || rejectedCause.isPresent())) {
            throw new IllegalArgumentException("An ACCEPTED ack requires externalRef and no rejectedCause");
        }
        if (status == AckStatus.REJECTED && (rejectedCause.isEmpty() || externalRef.isPresent())) {
            throw new IllegalArgumentException("A REJECTED ack requires rejectedCause and no externalRef");
        }
    }
}
