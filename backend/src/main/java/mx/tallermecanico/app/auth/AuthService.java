package mx.tallermecanico.app.auth;

import mx.tallermecanico.app.audit.AuditService;
import mx.tallermecanico.app.common.Texto;
import mx.tallermecanico.app.seguridad.SesionService;
import mx.tallermecanico.app.usuarios.strategy.UserCreationService;
import mx.tallermecanico.app.auth.strategy.RecoveryDeliveryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final ResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final UserCreationService userCreation;
    private final java.util.List<RecoveryDeliveryStrategy> recoveryStrategies;
    private final AuditService audit;
    private final SesionService sesiones;

    @Value("${app.base-url}") private String baseUrl;
    @Value("${app.recuperacion.minutos-vigencia:15}") private long minutosVigencia;

    /** Construye el servicio de autenticación con estrategias de creación y entrega.
     * @param users repositorio de cuentas
     * @param tokens repositorio de tokens de recuperación
     * @param encoder codificador de contraseñas BCrypt
     * @param userCreation Strategy resolver para registro por rol
     * @param recoveryStrategies Strategies para enviar enlaces
     * @param audit servicio de auditoría
     * @param sesiones administrador de sesiones activas
     */
    public AuthService(UserRepository users, ResetTokenRepository tokens, PasswordEncoder encoder,
                       UserCreationService userCreation, java.util.List<RecoveryDeliveryStrategy> recoveryStrategies,
                       AuditService audit, SesionService sesiones) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.userCreation = userCreation;
        this.recoveryStrategies = java.util.List.copyOf(recoveryStrategies);
        this.audit = audit;
        this.sesiones = sesiones;
    }

    /** Registra una cuenta pública mediante la estrategia asociada al rol público.
     * @param emailCrudo correo recibido del formulario
     * @param nombreCrudo nombre recibido del formulario
     * @param password contraseña validada que se convertirá a BCrypt
     * @param confirmacion confirmación de la contraseña
     * @return cuenta cliente persistida
     * @throws ResponseStatusException si las contraseñas no coinciden o el correo ya existe
     */
    /** Cambia una contraseña después de verificar la clave actual.
     * @param email correo autenticado
     * @param actual contraseña actual en texto transitorio
     * @param nueva contraseña nueva validada
     * @param confirmacion confirmación de la contraseña nueva
     * @return No aplica; el método completa la actualización antes de retornar.
     * @throws ResponseStatusException si la clave actual no es correcta o la nueva incumple reglas
     */
    @Transactional
    public UserAccount registrar(String emailCrudo, String nombreCrudo, String password, String confirmacion) {
        return userCreation.createPublic(emailCrudo, nombreCrudo, password, confirmacion);
    }

    /** Registra una cuenta pública con un teléfono opcional aún no verificado.
     * @param emailCrudo correo del formulario
     * @param nombreCrudo nombre del formulario
     * @param password contraseña validada
     * @param confirmacion confirmación de contraseña
     * @param telefono teléfono E.164 que no se considera verificado
     * @return cuenta cliente persistida
     */
    @Transactional
    public UserAccount registrar(String emailCrudo, String nombreCrudo, String password, String confirmacion, String telefono) {
        return userCreation.createPublic(emailCrudo, nombreCrudo, password, confirmacion, telefono);
    }

    @Transactional
    public void cambiarContrasena(String email, String actual, String nueva, String confirmacion) {
        UserAccount u = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tu sesión ya no es válida. Inicia sesión de nuevo."));
        if (!encoder.matches(actual, u.getPasswordHash())) {
            audit.recordPasswordChange(email, true, "Rechazado: contraseña actual incorrecta");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta");
        }
        if (!nueva.equals(confirmacion)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña y su confirmación no coinciden");
        }
        if (encoder.matches(nueva, u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser diferente a la actual");
        }
        u.updatePasswordHash(encoder.encode(nueva));
        users.save(u);
        audit.recordPasswordChange(email, true, "Contraseña actualizada");
        sesiones.expirarSesiones(email);
    }

    /** Solicita recuperación mediante el canal de correo compatible hacia atrás.
     * @param emailCrudo correo escrito por el usuario
     * @return No aplica; la notificación se solicita al canal EMAIL.
     * @throws ResponseStatusException si la entrega falla de forma irrecuperable
     */
    @Transactional
    public void solicitarRecuperacion(String emailCrudo) {
        solicitarRecuperacion(emailCrudo, "EMAIL");
    }

    /** Genera un token temporal y delega la entrega al canal de recuperación seleccionado.
     * Cuando el canal no tiene infraestructura configurada (sin servidor SMTP, por ejemplo),
     * devuelve el enlace para que la interfaz lo muestre al solicitante y la recuperación
     * no quede bloqueada en entornos sin proveedor de correo.
     * @param emailCrudo correo escrito por el usuario
     * @param requestedChannel canal elegido, EMAIL si es null
     * @return el enlace de restablecimiento solo en modo sin canal configurado; null cuando se envió
     * @throws ResponseStatusException si el canal solicitado no está registrado
     */
    @Transactional
    public String solicitarRecuperacion(String emailCrudo, String requestedChannel) {
        String channel = requestedChannel == null || requestedChannel.isBlank() ? "EMAIL" : requestedChannel.trim().toUpperCase(java.util.Locale.ROOT);
        RecoveryDeliveryStrategy delivery = recoveryStrategies.stream().filter(s -> s.supports(channel)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Canal de recuperación no disponible"));
        String email = Texto.correo(emailCrudo);
        java.util.Optional<UserAccount> opt = users.findByEmail(email).filter(UserAccount::isEnabled);
        if (opt.isEmpty()) {
            // Responder de forma genérica evita que la solicitud revele si el correo pertenece a una cuenta.
            audit.record("USER_ACCOUNT", null, "RECUPERACION_SIN_CUENTA", email, "Correo no registrado o inactivo");
            return null;
        }
        UserAccount u = opt.get();
        tokens.findByUserAndUsedAtIsNull(u).forEach(PasswordResetToken::markUsed);
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        tokens.save(new PasswordResetToken(hash(raw), u, Instant.now().plus(Duration.ofMinutes(minutosVigencia))));
        String enlace = baseUrl + "/reset-password?token=" + raw;
        if (!delivery.disponible()) {
            log.warn("[Recuperación] Canal {} sin configurar; el enlace se devuelve en modo local (cuenta id={})", channel, u.getId());
            audit.record("USER_ACCOUNT", u.getId(), "RECUPERACION_MODO_SIN_CANAL", email, "canal=" + channel);
            return enlace;
        }
        try {
            delivery.deliver(u, enlace);
            audit.record("USER_ACCOUNT", u.getId(), "RECUPERACION_SOLICITADA", email, "canal=" + channel);
        } catch (ResponseStatusException ex) {
            // Responder de forma genérica evita que la solicitud revele si el correo pertenece a una cuenta.
            log.warn("[Recuperación] Entrega no disponible por canal {} (cuenta id={})", channel, u.getId());
            audit.record("USER_ACCOUNT", u.getId(), "RECUPERACION_NO_ENTREGADA", email, "canal=" + channel);
        }
        return null;
    }

    /** Comprueba si un token existe, no se usó y continúa dentro de vigencia.
     * @param raw token presentado en el enlace
     * @return true cuando puede usarse para cambiar la contraseña
     */
    @Transactional(readOnly = true)
    public boolean tokenValido(String raw) {
        if (raw == null || raw.isBlank()) return false;
        return tokens.findByTokenHash(hash(raw))
                .filter(t -> t.getUsedAt() == null && t.getExpiresAt().isAfter(Instant.now()))
                .isPresent();
    }

    /** Reemplaza el hash de contraseña y consume el token en la misma transacción.
     * @param raw token recibido por el enlace
     * @param nueva contraseña validada
     * @param confirmacion confirmación de contraseña
     * @return No aplica; persiste la nueva credencial y consume el token.
     * @throws ResponseStatusException si el token venció, se usó o la clave no cumple
     */
    @Transactional
    public void restablecer(String raw, String nueva, String confirmacion) {
        if (!nueva.equals(confirmacion)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las contraseñas no coinciden");
        }
        PasswordResetToken t = tokens.findByTokenHash(hash(raw))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El enlace es inválido. Solicita uno nuevo."));
        if (t.getUsedAt() != null || t.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace ya fue usado o venció. Solicita uno nuevo.");
        }
        UserAccount user = t.getUser();
        if (encoder.matches(nueva, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser diferente a la anterior");
        }
        user.updatePasswordHash(encoder.encode(nueva));
        users.save(user);
        t.markUsed();
        tokens.save(t);
        audit.recordPasswordChange(user.getEmail(), false, "Contraseña restablecida con enlace de recuperación");
        sesiones.expirarSesiones(user.getEmail());
    }

    /** Calcula SHA-256 hexadecimal para localizar un token aleatorio sin guardar el original.
     * @param value token recibido
     * @return digest hexadecimal
     * @throws IllegalStateException si la JVM no dispone de SHA-256
     */
    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
