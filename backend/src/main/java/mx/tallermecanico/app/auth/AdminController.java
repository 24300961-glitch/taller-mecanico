package mx.tallermecanico.app.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mx.tallermecanico.app.audit.AuditService;
import mx.tallermecanico.app.common.Texto;
import mx.tallermecanico.app.seguridad.Permiso;
import mx.tallermecanico.app.seguridad.Rol;
import mx.tallermecanico.app.seguridad.RolRepository;
import mx.tallermecanico.app.seguridad.SesionService;
import mx.tallermecanico.app.usuarios.strategy.UserCreationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminController {

    private final UserRepository users;
    private final RolRepository roles;
    private final AuditService audit;
    private final SesionService sesiones;
    private final UserCreationService userCreation;

    /** Construye el controlador con los servicios de administración y alta de usuarios.
     * @param users repositorio de usuarios
     * @param roles repositorio de roles
     * @param audit servicio de auditoría
     * @param sesiones servicio de expiración de sesiones
     * @param userCreation servicio que selecciona la estrategia de alta por rol
     */
    public AdminController(UserRepository users, RolRepository roles, AuditService audit, SesionService sesiones,
                           UserCreationService userCreation) {
        this.users = users;
        this.roles = roles;
        this.audit = audit;
        this.sesiones = sesiones;
        this.userCreation = userCreation;
    }

    public record AsignarRolRequest(@NotNull(message = "Selecciona un rol") Long rolId) {}

    /** Datos editables de una cuenta existente.
     * @param fullName nombre visible
     * @param email correo de acceso normalizado
     */
    public record EditarUsuarioRequest(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 100) String fullName,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Email @jakarta.validation.constraints.Size(max = 190) String email) {}

    /** Datos para crear una cuenta desde el panel administrativo.
     * @param email correo de acceso
     * @param fullName nombre visible
     * @param password contraseña sometida a BCrypt antes de persistir
     * @param confirmPassword confirmación de contraseña
     * @param recoveryPhone teléfono E.164 opcional que comienza sin verificar
     * @param rolId rol que selecciona el administrador
     */
    public record CrearUsuarioRequest(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Email @jakarta.validation.constraints.Size(max = 190) String email,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 100) String fullName,
            @jakarta.validation.constraints.NotBlank @ContrasenaSegura String password,
            @jakarta.validation.constraints.NotBlank String confirmPassword,
            @jakarta.validation.constraints.Pattern(regexp = "^$|^\\+[1-9]\\d{7,14}$", message = "Usa formato internacional E.164") String recoveryPhone,
            @NotNull Long rolId) {}

    /** Crea una cuenta delegando en la estrategia correspondiente al rol solicitado.
     * @param request datos validados del nuevo usuario
     * @return identificador, correo, nombre y rol de la cuenta creada
     * @throws ResponseStatusException si el rol es interno o no existe, o el correo ya está usado
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLES_ASIGNAR')")
    public Map<String, Object> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        UserAccount created = userCreation.createForRole(request.email(), request.fullName(), request.password(),
                request.confirmPassword(), request.recoveryPhone(), request.rolId());
        return vista(created);
    }

    /** Actualiza nombre y correo de una cuenta existente.
     * @param id identificador de la cuenta
     * @param request datos validados del formulario
     * @param authentication usuario que ejecuta la edición
     * @return perfil actualizado y mensaje de resultado
     * @throws ResponseStatusException si la cuenta no existe, el correo está duplicado o el rol no permite editar cuentas administrativas
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USUARIOS_EDITAR','ROLES_ASIGNAR')")
    @Transactional
    public Map<String, Object> editar(@PathVariable Long id, @Valid @RequestBody EditarUsuarioRequest request,
                                      Authentication authentication) {
        UserAccount user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario seleccionado no existe"));
        if (esCuentaAdministrativa(user) && !tieneAutoridad(authentication, "ROLES_ASIGNAR")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un administrador puede editar cuentas administrativas.");
        }
        String emailAnterior = user.getEmail();
        String nombreAnterior = user.getFullName();
        String emailNuevo = Texto.correo(request.email());
        if (emailNuevo == null || emailNuevo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo es obligatorio.");
        }
        if (!emailNuevo.equals(emailAnterior) && users.existsByEmail(emailNuevo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese correo ya pertenece a otra cuenta.");
        }
        user.setFullName(request.fullName());
        user.setEmail(emailNuevo);
        users.save(user);
        audit.record("USER_ACCOUNT", user.getId(), "USUARIO_EDITADO", user.getEmail(),
                "nombre: " + nombreAnterior + " -> " + user.getFullName()
                        + "; correo: " + emailAnterior + " -> " + user.getEmail());
        if (!emailAnterior.equals(emailNuevo)) {
            sesiones.expirarSesiones(emailAnterior);
        }
        Map<String, Object> r = vista(user);
        r.put("message", "Datos de " + user.getFullName() + " actualizados.");
        return r;
    }

    /** Lista cuentas disponibles para administración.
     * @return perfiles resumidos sin hashes ni teléfonos
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('USUARIOS_CONSULTAR','ROLES_ASIGNAR')")
    public List<Map<String, Object>> listar() {
        return users.findAllByOrderByFullNameAsc().stream().map(AdminController::vista).toList();
    }

    /** Cambia el rol y expira las sesiones del usuario para actualizar permisos.
     * @param id identificador de cuenta
     * @param req identificador del nuevo rol
     * @param authentication administrador que ejecuta el cambio
     * @return perfil actualizado y mensaje de resultado
     * @throws ResponseStatusException si la cuenta o rol no existe o se intenta un cambio inseguro
     */
    @PatchMapping("/{id}/rol")
    @PreAuthorize("hasAuthority('ROLES_ASIGNAR')")
    @Transactional
    public Map<String, Object> asignarRol(@PathVariable Long id, @Valid @RequestBody AsignarRolRequest req,
                                          Authentication authentication) {
        UserAccount user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario seleccionado no existe"));
        Rol nuevo = roles.findById(req.rolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El rol seleccionado no existe"));
        if (!nuevo.isAsignable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El rol \"" + nuevo.getNombre() + "\" es interno y no se puede asignar a usuarios");
        }
        if (!nuevo.isActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El rol \"" + nuevo.getNombre() + "\" está suspendido. Reactívalo antes de asignarlo.");
        }
        if (user.getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes cambiar tu propio rol. Solicítalo a otro administrador.");
        }
        Rol anterior = user.getRol();
        if (anterior != null && anterior.getId().equals(nuevo.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, user.getFullName() + " ya tiene el rol \"" + nuevo.getNombre() + "\"");
        }
        if (anterior != null && anterior.tiene(Permiso.ROLES_CREAR) && !nuevo.tiene(Permiso.ROLES_CREAR)
                && user.isEnabled() && users.contarActivosConPermiso(Permiso.ROLES_CREAR) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede quitar el último usuario con permisos de administración de roles");
        }
        user.asignarRol(nuevo);
        users.save(user);
        audit.record("USER_ACCOUNT", user.getId(), "ROL_ASIGNADO", user.getEmail(),
                "de=" + (anterior == null ? "(sin rol)" : anterior.getNombre()) + " a=" + nuevo.getNombre());
        sesiones.expirarSesiones(user.getEmail());
        Map<String, Object> r = vista(user);
        r.put("message", "Rol \"" + nuevo.getNombre() + "\" asignado a " + user.getFullName()
                + ". Deberá iniciar sesión de nuevo para aplicar los permisos.");
        return r;
    }

    /** Suspende una cuenta para impedir su acceso sin borrar sus datos.
     * @param id identificador de la cuenta
     * @param authentication administrador que ejecuta la acción
     * @return perfil actualizado y mensaje de resultado
     */
    @PatchMapping("/{id}/suspender")
    @PreAuthorize("hasAnyAuthority('USUARIOS_SUSPENDER','ROLES_ASIGNAR')")
    @Transactional
    public Map<String, Object> suspender(@PathVariable Long id, Authentication authentication) {
        return cambiarEstatus(id, authentication, false);
    }

    /** Reactiva una cuenta previamente suspendida.
     * @param id identificador de la cuenta
     * @param authentication administrador que ejecuta la acción
     * @return perfil actualizado y mensaje de resultado
     */
    @PatchMapping("/{id}/reactivar")
    @PreAuthorize("hasAnyAuthority('USUARIOS_SUSPENDER','ROLES_ASIGNAR')")
    @Transactional
    public Map<String, Object> reactivar(@PathVariable Long id, Authentication authentication) {
        return cambiarEstatus(id, authentication, true);
    }

    /** Aplica el cambio de estatus con las protecciones de la propia cuenta y del último administrador.
     * @param id identificador de la cuenta
     * @param authentication administrador que ejecuta la acción
     * @param activar true para reactivar; false para suspender
     * @return perfil actualizado y mensaje de resultado
     * @throws ResponseStatusException si la cuenta no existe, ya está en ese estado o es la propia cuenta
     */
    private Map<String, Object> cambiarEstatus(Long id, Authentication authentication, boolean activar) {
        UserAccount user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario seleccionado no existe"));
        if (esCuentaAdministrativa(user) && !tieneAutoridad(authentication, "ROLES_ASIGNAR")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un administrador puede suspender o reactivar cuentas administrativas.");
        }
        if (user.isEnabled() == activar) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    activar ? "La cuenta ya está activa." : "La cuenta ya está suspendida.");
        }
        if (!activar && user.getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes suspender tu propia cuenta.");
        }
        if (!activar && user.getRol() != null) {
            boolean ultimoAdministrador = (user.getRol().tiene(Permiso.ROLES_CREAR)
                        && users.contarActivosConPermiso(Permiso.ROLES_CREAR) <= 1)
                    || (user.getRol().tiene(Permiso.ROLES_ASIGNAR)
                        && users.contarActivosConPermiso(Permiso.ROLES_ASIGNAR) <= 1);
            if (ultimoAdministrador) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No se puede suspender al último usuario con permisos de administración.");
            }
        }
        user.setEnabled(activar);
        users.save(user);
        audit.record("USER_ACCOUNT", user.getId(), activar ? "USUARIO_REACTIVADO" : "USUARIO_SUSPENDIDO",
                user.getEmail(), "estado=" + (activar ? "activo" : "suspendido"));
        if (!activar) sesiones.expirarSesiones(user.getEmail());
        Map<String, Object> r = vista(user);
        r.put("message", activar
                ? "Cuenta de " + user.getFullName() + " reactivada. Ya puede iniciar sesión."
                : "Cuenta de " + user.getFullName() + " suspendida. Sus sesiones abiertas se cerraron.");
        return r;
    }

    /** Indica si la cuenta tiene permisos de administración.
     * @param u cuenta a evaluar
     * @return true cuando puede crear roles o asignarlos
     */
    private static boolean esCuentaAdministrativa(UserAccount u) {
        return u.getRol() != null
                && (u.getRol().tiene(Permiso.ROLES_ASIGNAR) || u.getRol().tiene(Permiso.ROLES_CREAR));
    }

    /** Comprueba una autoridad concreta en la sesión actual.
     * @param authentication sesión autenticada
     * @param autoridad permiso buscado
     * @return true si la sesión tiene ese permiso
     */
    private static boolean tieneAutoridad(Authentication authentication, String autoridad) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> autoridad.equals(a.getAuthority()));
    }

    /** Convierte una cuenta en su representación pública administrativa.
     * @param u cuenta cargada desde base de datos
     * @return mapa de campos no secretos
     */
    static Map<String, Object> vista(UserAccount u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("email", u.getEmail());
        m.put("fullName", u.getFullName());
        m.put("rolId", u.getRol() == null ? null : u.getRol().getId());
        m.put("rol", u.getRol() == null ? null : u.getRol().getNombre());
        m.put("enabled", u.isEnabled());
        m.put("ultimoAcceso", u.getLastLoginAt() == null ? null : u.getLastLoginAt().toString());
        m.put("registro", u.getCreatedAt() == null ? null : u.getCreatedAt().toString());
        return m;
    }
}
