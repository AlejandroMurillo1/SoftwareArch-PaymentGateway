package com.apexstore.devtools;

import com.apexstore.generated.PaymentGateway.ResultNotification;
import com.apexstore.generated.PaymentGateway.TransactionNotifier;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Procesador FALSO para probar la pasarela sin el servidor E-Commerce real.
 * Escucha en el puerto 10001 e imprime cada resultado que recibe.
 *
 * Variable de entorno opcional FAIL_FIRST=N: las primeras N entregas de cada
 * transaccion fallan a proposito, para ver los reintentos del ResultNotifier.
 */
public class FakeProcessor {

    private static final String ENDPOINT = "tcp -h 0.0.0.0 -p 10001";
    private static final String IDENTITY = "TransactionNotifier";

    public static void main(String[] args) {
        int failFirst = Integer.parseInt(System.getenv().getOrDefault("FAIL_FIRST", "0"));

        try (Communicator communicator = Util.initialize(args)) {
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints("FakeProcessor", ENDPOINT);
            adapter.add(new Notifier(failFirst), Util.stringToIdentity(IDENTITY));
            adapter.activate();

            Thread mainThread = Thread.currentThread();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                communicator.shutdown();
                try {
                    mainThread.join();
                } catch (InterruptedException ignored) {
                }
            }));

            System.out.println("FakeProcessor listening on " + ENDPOINT + " (FAIL_FIRST=" + failFirst + "). Ctrl+C to stop.");
            communicator.waitForShutdown();
        }
    }

    private static class Notifier implements TransactionNotifier {
        private final int failFirst;
        private final Map<Integer, Integer> attemptsByTx = new ConcurrentHashMap<>();

        Notifier(int failFirst) {
            this.failFirst = failFirst;
        }

        @Override
        public void notifyTransactionResult(ResultNotification result, Current current) {
            int attempt = attemptsByTx.merge(result.transactionId, 1, Integer::sum);

            if (attempt <= failFirst) {
                System.out.printf("[PROCESSOR] tx=%d attempt %d -> simulated FAILURE%n", result.transactionId, attempt);
                throw new RuntimeException("simulated failure");
            }

            System.out.printf("[PROCESSOR] tx=%d status=%s ref=%s cause=%s (delivery #%d)%n",
                    result.transactionId,
                    result.status,
                    result.externalRef,
                    result.failureCause.isEmpty() ? "-" : result.failureCause,
                    attempt);
        }
    }
}
