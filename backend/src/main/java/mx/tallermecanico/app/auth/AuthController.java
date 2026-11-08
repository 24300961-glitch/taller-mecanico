package mx.tallermecanico.app.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;
import mx.tallermecanico.app.seguridad.SeguridadProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String PATRON_NOMBRE = "^[\\p{L}][\\p{L} .'-]{1,99}$";

    private final AuthService auth;
    private final UserRepository users;
    private final SeguridadProperties seguridad;

    public AuthController(AuthService auth, UserRepository users, SeguridadProperties seguridad) {
        this.auth = auth;
        this.users = users;
        this.seguridad = seguridad;
    }

    public record RegisterRequest(
            @NotBlank(message = "El correo es obligatorio") @Email(message = "Ingresa un correo válido")
            @Size(max = 190, message = "El correo no debe exceder 190 caracteres") String email,
            @NotBlank(message = "El nombre es obligatorio")
            @Pattern(regexp = PATRON_NOMBRE, message = "El nombre debe tener de 2 a 100 caracteres y solo letras, espacios, puntos, apóstrofos o guiones") String fullName,
            @NotBlank(message = "La contraseña es obligatoria") @ContrasenaSegura String password,
            @NotBlank(message = "Confirma tu contraseña") String confirmPassword,
            @jakarta.validation.constraints.Pattern(regexp = "^$|^\\+[1-9]\\d{7,14}$", message = "Usa formato internacional, por ejemplo +525512345678") String recoveryPhone) {}

    public record ForgotRequest(
            @NotBlank(message = "El correo es obligatorio") @Email(message = "Ingresa un correo válido") String email,
            String channel) {}

    public record ResetRequest(
            @NotBlank(message = "El enlace de recuperación es inválido") String token,
            @NotBlank(message = "La nueva contraseña es obligatoria") @ContrasenaSegura String newPassword,
            @NotBlank(message = "Confirma la nueva contraseña") String confirmPassword) {}

    public record ChangeRequest(
            @NotBlank(message = "La contraseña actual es obligatoria") String currentPassword,
            @NotBlank(message = "La nueva contraseña es obligatoria") @ContrasenaSegura String newPassword,
            @NotBlank(message = "Confirma la nueva contraseña") String confirmPassword) {}

    /** Devuelve el token CSRF requerido por los formularios de la API.
     * @param token token creado por el filtro CSRF
     * @return token y nombre de cabecera
     */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "header", token.getHeaderName());
    }

    /** Devuelve la etiqueta del rol con el que se crean las cuentas públicas.
     * @return rol de registro público configurado
     */
    @GetMapping("/registro-info")
    public Map<String, String> registroInfo() {
        return Map.of("rolRegistro", seguridad.getRolRegistroPublico());
    }

    /** Indica si un token de recuperación sigue vigente sin consumirlo.
     * @param token token temporal de la URL
     * @return vigencia del token
     */
    @GetMapping("/validar-token")
    public Map<String, Boolean> validarToken(@RequestParam(value = "token", required = false) String token) {
        return Map.of("valido", auth.tokenValido(token));
    }

    /** Registra una cuenta pública usando la estrategia asociada al rol de registro.
     * @param r datos de cuenta validados
     * @param authentication sesión actual, si existe
     * @return mensaje y ruta de acceso
     * @throws ResponseStatusException si existe sesión previa o falla la regla de alta
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(@Valid @RequestBody RegisterRequest r, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes una sesión iniciada. Cierra sesión para registrar otra cuenta.");
        }
        auth.registrar(r.email(), r.fullName(), r.password(), r.confirmPassword(), r.recoveryPhone());
        return Map.of("message", "Cuenta creada correctamente. Inicia sesión con tu correo y contraseña.",
                "redirect", "/login?registro=ok");
    }

    /** Devuelve identidad, rol y permisos de la sesión actual.
     * @param authentication principal autenticado
     * @return perfil serializable para la interfaz
     * @throws ResponseStatusException si la cuenta asociada ya no existe
     */
    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        UserAccount u = users.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tu sesión ya no es válida"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("email", u.getEmail());
        m.put("fullName", u.getFullName());
        m.put("rol", u.getRol() == null ? null : u.getRol().getNombre());
        m.put("permisos", authentication.getAuthorities().stream().map(a -> a.getAuthority())
                .filter(a -> !a.startsWith("ROLE_")).toList());
        return m;
    }


    /** Inicia recuperación delegando al canal Strategy solicitado.
     * @param r correo y canal; cuando el canal se omite se usa EMAIL
     * @return respuesta genérica para no confirmar si existe la cuenta; en modo sin canal
     *         configurado incluye el enlace para que la persona pueda restablecer igualmente
     */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> forgot(@Valid @RequestBody ForgotRequest r) {
        String enlace = auth.solicitarRecuperacion(r.email(), r.channel());
        if (enlace != null) {
            return Map.of(
                    "message", "Este entorno no tiene servidor de correo configurado, así que el enlace se muestra aquí. Úsalo para restablecer tu contraseña.",
                    "enlace", enlace);
        }
        return Map.of("message", "Si la cuenta existe y el canal está disponible, recibirás instrucciones para restablecer tu contraseña.");
    }

    /** Restablece la contraseña y consume el token temporal.
     * @param r token y nueva clave con confirmación
     * @return mensaje y ruta a login
     */
    @PostMapping("/reset-password")
    public Map<String, String> reset(@Valid @RequestBody ResetRequest r) {
        auth.restablecer(r.token(), r.newPassword(), r.confirmPassword());
        return Map.of("message", "Contraseña actualizada. Inicia sesión con tu nueva contraseña.",
                "redirect", "/login?reset=true");
    }
}
