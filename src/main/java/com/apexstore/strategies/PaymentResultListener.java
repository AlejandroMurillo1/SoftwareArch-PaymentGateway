package com.apexstore.strategies;

import com.apexstore.strategies.data.PaymentSummary;

public interface PaymentResultListener {
    void reportResult(PaymentSummary summary);
}
