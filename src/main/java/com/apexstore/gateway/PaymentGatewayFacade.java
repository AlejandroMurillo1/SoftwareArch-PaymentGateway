package com.apexstore.gateway;

import com.apexstore.strategies.PaymentStrategy;
import com.apexstore.strategies.data.PaymentAck;
import com.apexstore.strategies.data.PaymentRequest;
import com.apexstore.strategies.data.ResponseFactory;

import java.util.Map;

public class PaymentGatewayFacade {
    private static final System.Logger LOG = System.getLogger(PaymentGatewayFacade.class.getName());

    private final Map<String, PaymentStrategy> strategyMap;

    public PaymentGatewayFacade(Map<String, PaymentStrategy> strategyMap) {
        this.strategyMap = Map.copyOf(strategyMap);
    }

    public PaymentAck beginPayment(String method, PaymentRequest request) {
        if (method == null || request == null) {
            return ResponseFactory.rejectedAck("Payment method and request are required.");
        }

        PaymentStrategy strategy = strategyMap.get(method.toUpperCase());
        if (strategy == null) {
            return ResponseFactory.rejectedAck("Payment method: " + method + " does not exist, retry with a valid one.");
        }

        try {
            return strategy.pay(request);
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.ERROR, "Strategy " + method + " failed for tx " + request.transactionId(), e);
            return ResponseFactory.rejectedAck("Payment method " + method + " is unavailable, try again later.");
        }
    }
}
