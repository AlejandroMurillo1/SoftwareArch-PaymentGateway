package com.apexstore.gateway;

import com.apexstore.strategies.PaymentResultListener;
import com.apexstore.strategies.data.PaymentSummary;

public class LoggingResultListener implements PaymentResultListener {
    @Override
    public void reportResult(PaymentSummary summary) {
        System.out.printf("[CALLBACK] thread=%s tx=%d status=%s ref=%s cause=%s%n",
                Thread.currentThread().getName(),
                summary.transactionId(),
                summary.paymentStatus(),
                summary.externalRef(),
                summary.failureCause().orElse("-"));
    }
}
