package mx.tallermecanico.app.auth.strategy;

/** Puerto de integración para un proveedor SMS (por ejemplo, Twilio o un gateway local). */
public interface SmsGateway {
    /** Envía texto a un destino telefónico E.164.
     * @param phoneNumber teléfono internacional verificado
     * @param message contenido del SMS
     * @throws Exception si el proveedor rechaza o no puede entregar el mensaje
     */
    void send(String phoneNumber, String message) throws Exception;
}
