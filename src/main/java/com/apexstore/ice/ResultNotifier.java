package com.apexstore.ice;

import com.apexstore.generated.PaymentGateway.ResultNotification;
import com.apexstore.generated.PaymentGateway.TransactionNotifierPrx;
import com.apexstore.strategies.PaymentResultListener;
import com.apexstore.strategies.data.PaymentSummary;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ResultNotifier implements PaymentResultListener {
    private static final System.Logger LOG = System.getLogger(ResultNotifier.class.getName());

    private static final int MAX_ATTEMPTS = 5;
    private static final long BASE_DELAY_MILLIS = 1_000; // 1s, 2s, 4s, 8s entre intentos

    private final TransactionNotifierPrx processor;
    private final ScheduledExecutorService scheduler;

    public ResultNotifier(TransactionNotifierPrx processor, ScheduledExecutorService scheduler) {
        this.processor = processor;
        this.scheduler = scheduler;
    }

    @Override
    public void reportResult(PaymentSummary summary) {
        send(PaymentMapper.toWire(summary), 1);
    }

    private void send(ResultNotification notification, int attempt) {
        try {
            processor.notifyTransactionResultAsync(notification).whenComplete((ignored, error) -> {
                if (error == null) {
                    LOG.log(System.Logger.Level.INFO,
                            "Result delivered: tx=" + notification.transactionId + " (attempt " + attempt + ")");
                } else {
                    retryOrGiveUp(notification, attempt, error);
                }
            });
        } catch (RuntimeException e) { // fallo al iniciar la invocacion
            retryOrGiveUp(notification, attempt, e);
        }
    }

    private void retryOrGiveUp(ResultNotification notification, int attempt, Throwable error) {
        if (attempt >= MAX_ATTEMPTS) {
            LOG.log(System.Logger.Level.ERROR,
                    "GIVING UP delivering result: tx=" + notification.transactionId
                            + " status=" + notification.status + " after " + attempt + " attempts", error);
            return;
        }

        long delay = BASE_DELAY_MILLIS << (attempt - 1);
        LOG.log(System.Logger.Level.WARNING,
                "Delivery failed: tx=" + notification.transactionId + " (attempt " + attempt
                        + "), retrying in " + delay + " ms. Cause: " + error);
        try {
            scheduler.schedule(() -> send(notification, attempt + 1), delay, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            LOG.log(System.Logger.Level.ERROR,
                    "Scheduler closed, result lost: tx=" + notification.transactionId, e);
        }
    }
}
