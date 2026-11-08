package mx.tallermecanico.app.auth.strategy;

import mx.tallermecanico.app.auth.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Envía el enlace de recuperación por correo SMTP. */
@Component
public final class EmailRecoveryStrategy implements RecoveryDeliveryStrategy {
    private static final Logger log = LoggerFactory.getLogger(EmailRecoveryStrategy.class);
    private final ObjectProvider<JavaMailSender> senderProvider;
    @Value("${spring.mail.host:}") private String smtpHost;

    /** Construye la estrategia con un proveedor SMTP opcional.
     * @param senderProvider proveedor de Spring Mail
     */
    public EmailRecoveryStrategy(ObjectProvider<JavaMailSender> senderProvider) { this.senderProvider = senderProvider; }

    /** Comprueba si el canal es EMAIL.
     * @param channel canal solicitado
     * @return true solo para EMAIL
     */
    @Override public boolean supports(String channel) { return "EMAIL".equalsIgnoreCase(channel); }

    /** Comprueba si hay servidor SMTP configurado.
     * @return true solo cuando existe host y remitente utilizable
     */
    @Override public boolean disponible() {
        JavaMailSender sender = senderProvider.getIfAvailable();
        return sender != null && smtpHost != null && !smtpHost.isBlank();
    }

    /** Envía el enlace a la dirección registrada.
     * @param account cuenta destinataria
     * @param resetUrl enlace con token aleatorio temporal
     * @throws ResponseStatusException con HTTP 503 si SMTP no está disponible
     */
    @Override public void deliver(UserAccount account, String resetUrl) {
        JavaMailSender sender = senderProvider.getIfAvailable();
        if (sender == null || smtpHost == null || smtpHost.isBlank()) {
            log.warn("[Recuperación] SMTP no configurado; se rechazó entregar recuperación por email");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El canal de correo no está disponible actualmente.");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(account.getEmail());
            message.setSubject("Recupera tu acceso");
            message.setText("Usa este enlace temporal para restablecer tu contraseña: " + resetUrl
                    + "\n\nSi no solicitaste este cambio, ignora el mensaje.");
            sender.send(message);
        } catch (Exception ex) {
            // No se incluye el enlace en el log para evitar filtrar el token de un solo uso.
            // Se registra el tipo de fallo, nunca el stacktrace del proveedor que podría incluir el cuerpo del correo.
            log.error("[Recuperación] No se pudo entregar email (cuenta id={}, error={})",
                    account.getId(), ex.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No fue posible enviar el correo en este momento.");
        }
    }
}
