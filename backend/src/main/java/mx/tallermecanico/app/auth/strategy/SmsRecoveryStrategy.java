package mx.tallermecanico.app.auth.strategy;

import mx.tallermecanico.app.auth.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Estrategia de entrega SMS. En esta fase no se envían SMS por teléfono;
 * el método siempre lanza 503 indicando que el canal no está configurado. */
@Component
public final class SmsRecoveryStrategy implements RecoveryDeliveryStrategy {
    private static final Logger log = LoggerFactory.getLogger(SmsRecoveryStrategy.class);
    private final ObjectProvider<SmsGateway> gatewayProvider;

    public SmsRecoveryStrategy(ObjectProvider<SmsGateway> gatewayProvider) { this.gatewayProvider = gatewayProvider; }

    @Override public boolean supports(String channel) { return "SMS".equalsIgnoreCase(channel); }

    /** El canal SMS no tiene proveedor en esta fase.
     * @return false siempre
     */
    @Override public boolean disponible() { return false; }

    @Override public void deliver(UserAccount account, String resetUrl) {
        log.info("[Recuperación] Canal SMS no configurado en esta fase para cuenta id={}", account.getId());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El canal SMS no está configurado en esta fase de la aplicación");
    }
}