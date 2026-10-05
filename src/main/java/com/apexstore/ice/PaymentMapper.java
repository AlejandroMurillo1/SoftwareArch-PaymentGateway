package com.apexstore.ice;

import com.apexstore.generated.PaymentGateway.AckResponse;
import com.apexstore.generated.PaymentGateway.PaymentOrder;
import com.apexstore.generated.PaymentGateway.ResultNotification;
import com.apexstore.strategies.data.AckStatus;
import com.apexstore.strategies.data.PaymentAck;
import com.apexstore.strategies.data.PaymentRequest;
import com.apexstore.strategies.data.PaymentStatus;
import com.apexstore.strategies.data.PaymentSummary;

import java.math.BigDecimal;
import java.util.UUID;

public final class PaymentMapper {

    private PaymentMapper() {
    }

    // ---- Slice -> dominio ----

    /** Lanza IllegalArgumentException (incluye NumberFormatException) si el monto o algun campo es invalido. */
    public static PaymentRequest toDomain(PaymentOrder order) {
        return new PaymentRequest(order.transactionId, new BigDecimal(order.amount), order.currency);
    }

    // ---- dominio -> Slice ----

    public static AckResponse toWire(PaymentAck ack) {
        return new AckResponse(
                ackStatusToWire(ack.status()),
                ack.externalRef().map(UUID::toString).orElse(""),
                ack.rejectedCause().orElse(""));
    }

    public static AckResponse rejectedAckResponse(String cause) {
        return new AckResponse(com.apexstore.generated.PaymentGateway.AckStatus.REJECTED, "", cause);
    }

    public static ResultNotification toWire(PaymentSummary summary) {
        return new ResultNotification(
                summary.transactionId(),
                paymentStatusToWire(summary.paymentStatus()),
                summary.externalRef().toString(),
                summary.failureCause().orElse(""));
    }

    private static com.apexstore.generated.PaymentGateway.AckStatus ackStatusToWire(AckStatus status) {
        return switch (status) {
            case ACCEPTED -> com.apexstore.generated.PaymentGateway.AckStatus.ACCEPTED;
            case REJECTED -> com.apexstore.generated.PaymentGateway.AckStatus.REJECTED;
        };
    }

    private static com.apexstore.generated.PaymentGateway.PaymentStatus paymentStatusToWire(PaymentStatus status) {
        return switch (status) {
            case CONFIRMED -> com.apexstore.generated.PaymentGateway.PaymentStatus.CONFIRMED;
            case FAILED -> com.apexstore.generated.PaymentGateway.PaymentStatus.FAILED;
        };
    }
}
