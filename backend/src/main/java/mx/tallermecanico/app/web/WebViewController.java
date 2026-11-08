package mx.tallermecanico.app.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

/** Sirve la SPA de React (`frontend/`, compilada en `static/index.html`).
 * Cada ruta conserva su exigencia de autoridad: Spring Security redirige a
 * `/login` cuando no hay sesión y responde 403 sin permiso. React impone las
 * mismas reglas en el cliente a partir de `GET /api/auth/me`. */
@Controller
public class WebViewController {

    /** Dirige la raíz al formulario adecuado o conserva un enlace de recuperación.
     * @param principal principal autenticado o null
     * @param resetToken token opcional de recuperación
     * @return redirección a la ruta SPA correspondiente
     */
    @GetMapping("/")
    public String root(Principal principal,
                       @RequestParam(value = "resetToken", required = false) String resetToken) {
        if (resetToken != null && !resetToken.isBlank()) {
            return "redirect:/reset-password?token=" + java.net.URLEncoder.encode(resetToken, java.nio.charset.StandardCharsets.UTF_8);
        }
        return principal != null ? "redirect:/dashboard" : "redirect:/login";
    }

    /** Entrega la SPA en la ruta de acceso.
     * @return reenvío a la SPA
     */
    @GetMapping("/login")
    public String login() {
        return "forward:/index.html";
    }

    /** Entrega la SPA en la ruta de registro.
     * @return reenvío a la SPA
     */
    @GetMapping("/registro")
    public String registro() {
        return "forward:/index.html";
    }

    /** Entrega la SPA en la ruta de recuperación.
     * @return reenvío a la SPA
     */
    @GetMapping("/recuperar")
    public String recuperar() {
        return "forward:/index.html";
    }

    /** Entrega la SPA en la ruta de restablecimiento.
     * @return reenvío a la SPA
     */
    @GetMapping("/reset-password")
    public String resetPassword() {
        return "forward:/index.html";
    }

    /** Entrega la SPA en el panel principal.
     * @return reenvío a la SPA
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "forward:/index.html";
    }

    /** Entrega la SPA en la lista administrativa de usuarios.
     * @return reenvío a la SPA
     */
    @GetMapping("/usuarios")
    @PreAuthorize("hasAuthority('USUARIOS_CONSULTAR')")
    public String usuarios() {
        return "forward:/index.html";
    }

    /** Entrega la SPA en gestión de roles.
     * @return reenvío a la SPA
     */
    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('ROLES_CREAR','ROLES_ASIGNAR')")
    public String asignarRoles() {
        return "forward:/index.html";
    }

    /** Entrega la SPA en el módulo de clientes.
     * @return reenvío a la SPA
     */
    @GetMapping("/clientes")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR_DE_TALLER','ROLE_RECEPCIONISTA')")
    public String clientes() {
        return "forward:/index.html";
    }

    /** Entrega la SPA en la pantalla de vehículos.
     * @return reenvío a la SPA
     */
    @GetMapping("/vehiculos")
    @PreAuthorize("hasAuthority('VEHICULOS_CONSULTAR')")
    public String vehiculos() {
        return "forward:/index.html";
    }

    /** Entrega la SPA en el historial de movimientos.
     * @return reenvío a la SPA
     */
    @GetMapping("/historial")
    @PreAuthorize("hasAuthority('VEHICULOS_CONSULTAR')")
    public String historial() {
        return "forward:/index.html";
    }

    /** Entrega la SPA en administración de talleres.
     * @return reenvío a la SPA
     */
    @GetMapping("/talleres")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR_DE_TALLER')")
    public String talleres() {
        return "forward:/index.html";
    }

    /** Entrega la SPA en la documentación técnica.
     * @return reenvío a la SPA
     */
    @GetMapping("/documentacion")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR_DE_TALLER')")
    public String documentacion() {
        return "forward:/index.html";
    }
}
