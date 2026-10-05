package com.apexstore.strategies;

import com.apexstore.strategies.data.PaymentAck;
import com.apexstore.strategies.data.PaymentRequest;
import com.apexstore.strategies.data.PaymentSummary;
import com.apexstore.strategies.data.ResponseFactory;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public class CryptoStrategy implements PaymentStrategy {
    private static final System.Logger LOG = System.getLogger(CryptoStrategy.class.getName());

    private static final long PROCESSING_MILLIS = 5_000;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("5000");

    private final PaymentResultListener listener;
    private final Executor executor;

    public CryptoStrategy(PaymentResultListener listener, Executor executor) {
        this.listener = listener;
        this.executor = executor;
    }

    @Override
    public PaymentAck pay(PaymentRequest request) {
        UUID externalRef = UUID.randomUUID();

        try{
            executor.execute(() -> process(request, externalRef));
        }catch(RejectedExecutionException re){
            return ResponseFactory.rejectedAck("Crypto/Blockchain payment system is busy, try again later");
        }

        return ResponseFactory.acceptedAck(externalRef);
    }

    private void process(PaymentRequest request, UUID externalRef){
        PaymentSummary summary;

        try{
            Thread.sleep(PROCESSING_MILLIS);

            if(request.amount().compareTo(MAX_AMOUNT) > 0){
                summary = ResponseFactory.failedSummary(request.transactionId(), externalRef, "Payment declined: limit exceeded");
            }else{
                summary = ResponseFactory.confirmedSummary(request.transactionId(), externalRef);
            }
        }catch (InterruptedException ie){
            Thread.currentThread().interrupt();
            summary = ResponseFactory.failedSummary(request.transactionId(), externalRef, "Processing interrupted");
        }catch (RuntimeException re){
            summary = ResponseFactory.failedSummary(request.transactionId(), externalRef, "Unexpected error");
        }

        try{
           listener.reportResult(summary);
        }catch(RuntimeException re){
            LOG.log(System.Logger.Level.ERROR, "Could not report result for tx " + request.transactionId(), re);
        }
    }
}
