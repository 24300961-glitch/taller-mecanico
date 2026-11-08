package mx.tallermecanico.app.usuarios.strategy;

import java.util.List;
import mx.tallermecanico.app.audit.AuditService;
import mx.tallermecanico.app.auth.UserAccount;
import mx.tallermecanico.app.auth.UserRepository;
import mx.tallermecanico.app.common.Texto;
import mx.tallermecanico.app.seguridad.Rol;
import mx.tallermecanico.app.seguridad.RolService;
import mx.tallermecanico.app.seguridad.SeguridadProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Selecciona una Strategy de creación según el rol que el flujo autorizado solicite. */
@Service
public class UserCreationService {
    private final List<CreacionUsuarioStrategy> strategies;
    private final UserRepository users;
    private final RolService roles;
    private final SeguridadProperties properties;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    /** Construye el servicio con dependencias de persistencia, hash y estrategias.
     * @param configuredStrategies Strategies base por rol configurado
     * @param extensionStrategies Strategies adicionales o especializadas
     * @param users repositorio de cuentas
     * @param roles servicio de catálogo de roles
     * @param properties configuración de rol público
     * @param encoder codificador BCrypt
     * @param audit servicio de auditoría
     */
    public UserCreationService(@Qualifier("userCreationStrategies") List<CreacionUsuarioStrategy> configuredStrategies,
                               ObjectProvider<CreacionUsuarioStrategy> extensionStrategies, UserRepository users, RolService roles,
                               SeguridadProperties properties, PasswordEncoder encoder, AuditService audit) {
        // Las Strategies explícitas tienen prioridad sobre el constructor genérico por rol.
        java.util.ArrayList<CreacionUsuarioStrategy> ordered = new java.util.ArrayList<>(extensionStrategies.orderedStream().toList());
        ordered.addAll(configuredStrategies);
        this.strategies = List.copyOf(ordered); this.users = users; this.roles = roles;
        this.properties = properties; this.encoder = encoder; this.audit = audit;
    }

    /** Crea una cuenta cliente desde el flujo público.
     * @param email correo ingresado
     * @param fullName nombre completo
     * @param password contraseña en texto transitorio para codificar
     * @param confirmation confirmación de contraseña
     * @return cuenta cliente persistida
     * @throws ResponseStatusException si datos duplicados o confirmación incorrecta
     */
    @Transactional public UserAccount createPublic(String email, String fullName, String password, String confirmation) {
        return createPublic(email, fullName, password, confirmation, null);
    }

    /** Crea una cuenta cliente e incorpora opcionalmente un teléfono no verificado.
     * @param email correo ingresado
     * @param fullName nombre completo
     * @param password contraseña en texto transitorio
     * @param confirmation confirmación de contraseña
     * @param recoveryPhone teléfono E.164 opcional; no se usa hasta verificarlo
     * @return cuenta cliente persistida
     * @throws ResponseStatusException si no coinciden las contraseñas o el correo existe
     */
    @Transactional public UserAccount createPublic(String email, String fullName, String password,
                                                    String confirmation, String recoveryPhone) {
        if (!password.equals(confirmation)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las contraseñas no coinciden");
        return create(email, fullName, password, Texto.limpiar(recoveryPhone),
                roles.obtenerPorNombre(properties.getRolRegistroPublico()), false);
    }

    /** Crea una cuenta con el rol solicitado por un operador autorizado.
     * @param email correo ingresado
     * @param fullName nombre completo
     * @param password contraseña en texto transitorio para codificar
     * @param roleId identificador de rol persistido
     * @param confirmation confirmación de contraseña
     * @return cuenta creada
     * @throws ResponseStatusException si el rol no existe, no es asignable o el correo está duplicado
     */
    @Transactional public UserAccount createForRole(String email, String fullName, String password, String confirmation, Long roleId) {
        return createForRole(email, fullName, password, confirmation, null, roleId);
    }

    /** Crea una cuenta de rol asignable con teléfono de recuperación aún no verificado.
     * @param email correo ingresado
     * @param fullName nombre completo
     * @param password contraseña transitoria
     * @param confirmation confirmación
     * @param recoveryPhone teléfono E.164 opcional
     * @param roleId rol persistido
     * @return cuenta creada
     * @throws ResponseStatusException si el correo, confirmación o rol no son válidos
     */
    @Transactional public UserAccount createForRole(String email, String fullName, String password, String confirmation,
                                                     String recoveryPhone, Long roleId) {
        if (!password.equals(confirmation)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las contraseñas no coinciden");
        Rol role = roles.obtener(roleId);
        if (!role.isAsignable()) throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pueden crear cuentas con un rol interno");
        return create(email, fullName, password, Texto.limpiar(recoveryPhone), role, true);
    }

    /** Normaliza, codifica, delega la construcción y persiste una cuenta.
     * @param email correo ingresado
     * @param fullName nombre ingresado
     * @param password contraseña en texto transitorio
     * @param role rol objetivo
     * @param auditAsAdministrative indica si la creación la solicitó el personal autorizado
     * @return cuenta persistida
     * @throws ResponseStatusException si correo duplicado o falta una estrategia para el rol
     */
    private UserAccount create(String email, String fullName, String password, String recoveryPhone, Rol role, boolean auditAsAdministrative) {
        String normalizedEmail = Texto.correo(email);
        if (users.existsByEmail(normalizedEmail)) throw duplicateEmail();
        CreacionUsuarioStrategy strategy = strategies.stream().filter(candidate -> candidate.soporta(role)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No hay una estrategia configurada para este rol"));
        UserAccount account = strategy.crear(normalizedEmail, Texto.limpiar(fullName), encoder.encode(password), recoveryPhone, role);
        try {
            UserAccount saved = users.saveAndFlush(account);
            audit.record("USER_ACCOUNT", saved.getId(), "USUARIO_REGISTRADO", normalizedEmail,
                    "rol=" + role.getNombre() + "; origen=" + (auditAsAdministrative ? "administrativo" : "público"));
            return saved;
        } catch (DataIntegrityViolationException ex) {
            // La restricción única de correo resuelve la condición de carrera entre registros concurrentes.
            throw duplicateEmail();
        }
    }

    /** Construye la respuesta de conflicto sin revelar datos de otra cuenta.
     * @return excepción HTTP 409
     */
    private static ResponseStatusException duplicateEmail() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Este correo ya tiene una cuenta. Inicia sesión o recupera tu contraseña.");
    }
}
