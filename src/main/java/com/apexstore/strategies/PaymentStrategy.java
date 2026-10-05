package com.apexstore.strategies;

import com.apexstore.strategies.data.PaymentAck;
import com.apexstore.strategies.data.PaymentRequest;

public interface PaymentStrategy {
    PaymentAck pay(PaymentRequest request);
}
