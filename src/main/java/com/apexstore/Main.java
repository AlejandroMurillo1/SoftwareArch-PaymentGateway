package com.apexstore;

import com.apexstore.gateway.PaymentGatewayFacade;
import com.apexstore.generated.PaymentGateway.TransactionNotifierPrx;
import com.apexstore.ice.PaymentGatewayServant;
import com.apexstore.ice.ResultNotifier;
import com.apexstore.strategies.*;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        try(Communicator communicator = Util.initialize(args)) {
            // 1) Proxy hacia el procesador (configurado por propiedad, nada fijo en el codigo)
            ObjectPrx base = communicator.propertyToProxy("TransactionNotifier.Proxy");
            if (base == null) {
                System.err.println("Missing property 'TransactionNotifier.Proxy' (check --Ice.Config)");
                return;
            }
            // uncheckedCast: no hace llamada remota, asi arrancamos aunque el procesador aun no este levantado
            TransactionNotifierPrx processor = TransactionNotifierPrx.uncheckedCast(base);

            // 2) Executors: trabajo lento de las strategies y reintentos del notifier
            ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
            ScheduledExecutorService retryScheduler = Executors.newSingleThreadScheduledExecutor();

            // 3) Listener real (reemplaza al LoggingResultListener)
            PaymentGatewayFacade facade = getPaymentGatewayFacade(processor, retryScheduler, workers);

            // 5) Servant y adapter de Ice (endpoint leido de la propiedad Gateway.Endpoints)
            ObjectAdapter adapter = communicator.createObjectAdapter("Gateway");
            adapter.add(new PaymentGatewayServant(facade), Util.stringToIdentity("PaymentGatewayService"));
            adapter.activate();

            // 6) Ctrl+C: pide el cierre de Ice y espera a que este hilo termine la limpieza
            Thread mainThread = Thread.currentThread();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutdown requested...");
                communicator.shutdown();
                try {
                    mainThread.join();
                } catch (InterruptedException ignored) {
                }
            }));

            System.out.println("PaymentGateway listening. Press Ctrl+C to stop.");
            communicator.waitForShutdown();

            // 7) Apagado ordenado. El communicator sigue abierto hasta salir del try,
            //    porque los reintentos del notifier lo necesitan para entregar resultados pendientes.
            shutdownGracefully("workers", workers, 30);
            shutdownGracefully("retryScheduler", retryScheduler, 40);
            System.out.println("PaymentGateway stopped.");

        }
    }

    private static PaymentGatewayFacade getPaymentGatewayFacade(TransactionNotifierPrx processor, ScheduledExecutorService retryScheduler, ExecutorService workers) {
        PaymentResultListener listener = new ResultNotifier(processor, retryScheduler);

        // 4) Strategies y Facade (unico lugar que conoce las concretas)
        Map<String, PaymentStrategy> strategies = Map.of(
                "STRIPE", new StripeStrategy(listener, workers),
                "PSE", new PseStrategy(listener, workers),
                "CRYPTO", new CryptoStrategy(listener, workers)
        );
        return new PaymentGatewayFacade(strategies);
    }

    private static void shutdownGracefully(String name, ExecutorService executor, long seconds)
            throws InterruptedException {
        executor.shutdown(); // no acepta tareas nuevas, deja terminar las en curso
        if (!executor.awaitTermination(seconds, TimeUnit.SECONDS)) {
            System.err.println("[" + name + "] timeout, forcing shutdown");
            executor.shutdownNow();
        }
    }
}
