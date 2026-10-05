package com.apexstore.devtools;

import com.apexstore.generated.PaymentGateway.AckResponse;
import com.apexstore.generated.PaymentGateway.PaymentGatewayServicePrx;
import com.apexstore.generated.PaymentGateway.PaymentOrder;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

/**
 * Cliente de prueba: hace de procesador "hacia adelante" y llama a beginGatewayPayment.
 * Los resultados finales NO se ven aqui: los imprime el FakeProcessor.
 */
public class TestClient {

    private static final String GATEWAY_PROXY = "PaymentGatewayService:tcp -h 127.0.0.1 -p 10000";

    public static void main(String[] args) {
        try (Communicator communicator = Util.initialize(args)) {
            ObjectPrx base = communicator.stringToProxy(GATEWAY_PROXY);
            PaymentGatewayServicePrx gateway = PaymentGatewayServicePrx.checkedCast(base);
            if (gateway == null) {
                System.err.println("Gateway proxy is not a PaymentGatewayService");
                return;
            }

            send(gateway, 1, "STRIPE", "100");    // se confirma
            send(gateway, 2, "STRIPE", "9000");   // falla (supera el limite simulado de Stripe)
            send(gateway, 3, "PSE", "250");
            send(gateway, 4, "CRYPTO", "300");
            send(gateway, 5, "NEQUI", "50");      // metodo inexistente -> REJECTED
            send(gateway, 6, "STRIPE", "abc");    // monto no numerico -> REJECTED
            send(gateway, 7, "STRIPE", "-5");     // monto negativo -> REJECTED
            send(gateway, 8, "", "50");           // metodo vacio -> REJECTED

            System.out.println("Done. Watch the FakeProcessor console for the final results.");
        }
    }

    private static void send(PaymentGatewayServicePrx gateway, int txId, String method, String amount) {
        AckResponse ack = gateway.beginGatewayPayment(new PaymentOrder(txId, method, amount, "COP"));
        System.out.printf("[ACK] tx=%d method='%s' amount=%s -> %s ref=%s cause=%s%n",
                txId, method, amount, ack.status,
                ack.externalRef.isEmpty() ? "-" : ack.externalRef,
                ack.rejectedCause.isEmpty() ? "-" : ack.rejectedCause);
    }
}
