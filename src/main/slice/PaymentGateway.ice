// Contrato Ice entre el Servidor E-Commerce (procesador) y el Servidor de Pasarelas.
// Version del contrato: 1.0  (sintaxis Slice de Ice 3.8)
//
// Convenciones:
//  - Los campos de texto opcionales viajan como cadena vacia "" cuando no aplican.
//  - amount es un decimal en texto, con punto como separador y sin separador de miles (ej. "250.50").
//  - method usa las claves exactas en mayusculas: "STRIPE", "PSE", "CRYPTO".
//  - Los errores de negocio se expresan con AckStatus.REJECTED, no con excepciones.

[["java:package:com.apexstore.generated"]]
module PaymentGateway
{
    /// Resultado de la solicitud inicial.
    enum AckStatus { ACCEPTED, REJECTED };

    /// Resultado final del pago.
    enum PaymentStatus { CONFIRMED, FAILED };

    /// Solicitud de cobro enviada por el procesador.
    struct PaymentOrder
    {
        int transactionId;
        string method;
        string amount;
        string currency;
    };

    /// Respuesta sincrona: solo indica si la solicitud fue aceptada para procesarse.
    struct AckResponse
    {
        AckStatus status;
        string externalRef;      // vacio si fue rechazada
        string rejectedCause;    // vacio si fue aceptada
    };

    /// Resultado final, enviado despues de forma asincrona.
    struct ResultNotification
    {
        int transactionId;
        PaymentStatus status;
        string externalRef;
        string failureCause;     // vacio si fue confirmada
    };

    /// La PROVEE la pasarela (Facade); la REQUIERE el procesador.
    interface PaymentGatewayService
    {
        /// Inicia un pago. Retorna solo el acuse; el resultado llega por TransactionNotifier.
        /// @param order La solicitud de cobro.
        /// @return El acuse de la solicitud.
        AckResponse beginGatewayPayment(PaymentOrder order);
    };

    /// La PROVEE el procesador; la REQUIERE la pasarela. Debe ser idempotente.
    interface TransactionNotifier
    {
        /// Notifica el resultado final de un pago.
        /// @param result El resultado final.
        void notifyTransactionResult(ResultNotification result);
    };
};
