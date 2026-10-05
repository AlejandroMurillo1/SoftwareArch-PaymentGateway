package com.apexstore.strategies.data;

import java.util.Optional;
import java.util.UUID;

public final class ResponseFactory {

    private ResponseFactory(){

    }

    public static PaymentAck acceptedAck(UUID externalRef) {
        return new PaymentAck(AckStatus.ACCEPTED, Optional.of(externalRef), Optional.empty());
    }

    public static PaymentAck rejectedAck(String cause) {
        return new PaymentAck(AckStatus.REJECTED, Optional.empty(), Optional.of(cause));
    }

    public static PaymentSummary confirmedSummary(int transactionId, UUID externalRef) {
        return new PaymentSummary(transactionId, PaymentStatus.CONFIRMED, externalRef, Optional.empty());
    }

    public static PaymentSummary failedSummary(int transactionId, UUID externalRef, String cause) {
        return new PaymentSummary(transactionId, PaymentStatus.FAILED, externalRef, Optional.of(cause));
    }

}
