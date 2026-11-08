package mx.tallermecanico.app.auth.strategy;

import mx.tallermecanico.app.auth.UserAccount;

/** Strategy independiente para entregar un enlace de recuperación por un canal. */
public interface RecoveryDeliveryStrategy {
    /** Indica si la estrategia sirve al canal pedido.
     * @param channel canal de entrega
     * @return true si esta estrategia procesa el canal
     */
    boolean supports(String channel);

    /** Indica si el canal tiene infraestructura configurada (por ejemplo, servidor SMTP).
     * Cuando devuelve false el servicio de recuperación devuelve el enlace directamente
     * al solicitante en lugar de intentar una entrega que está condenada al fallo.
     * @return true cuando el canal puede entregar enlaces por sí mismo
     */
    boolean disponible();

    /** Entrega un enlace temporal sin almacenar ni registrar su valor en texto plano.
     * @param account cuenta propietaria del enlace
     * @param resetUrl URL completa, válida y de un solo uso
     * @throws org.springframework.web.server.ResponseStatusException si el canal no está disponible
     */
    void deliver(UserAccount account, String resetUrl);
}
