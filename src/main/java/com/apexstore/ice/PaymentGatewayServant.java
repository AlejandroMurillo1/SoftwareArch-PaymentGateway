package com.apexstore.ice;

import com.apexstore.gateway.PaymentGatewayFacade;
import com.apexstore.generated.PaymentGateway.AckResponse;
import com.apexstore.generated.PaymentGateway.PaymentGatewayService;
import com.apexstore.generated.PaymentGateway.PaymentOrder;
import com.apexstore.strategies.data.PaymentAck;
import com.apexstore.strategies.data.PaymentRequest;
import com.zeroc.Ice.Current;

public class PaymentGatewayServant implements PaymentGatewayService {

    private final PaymentGatewayFacade facade;

    public PaymentGatewayServant(PaymentGatewayFacade facade) {
        this.facade = facade;
    }

    @Override
    public AckResponse beginGatewayPayment(PaymentOrder order, Current current) {
        PaymentRequest request;
        try {
            request = PaymentMapper.toDomain(order);
        } catch (IllegalArgumentException e) { // monto no numerico, monto <= 0, moneda vacia...
            return PaymentMapper.rejectedAckResponse("Invalid payment order: " + e.getMessage());
        }

        PaymentAck ack = facade.beginPayment(order.method, request);
        return PaymentMapper.toWire(ack);
    }
}
