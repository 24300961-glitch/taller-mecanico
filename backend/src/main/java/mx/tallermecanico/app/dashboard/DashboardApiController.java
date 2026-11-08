package mx.tallermecanico.app.dashboard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mx.tallermecanico.app.auth.UserAccount;
import mx.tallermecanico.app.auth.UserRepository;
import mx.tallermecanico.app.common.Texto;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Expone únicamente la identidad y el rol para el panel compacto de la SPA. */
@RestController
@RequestMapping("/api")
public class DashboardApiController {

    private final UserRepository users;
    /** Construye el controlador con el repositorio de cuentas.
     * @param users repositorio de cuentas
     */
    public DashboardApiController(UserRepository users) {
        this.users = users;
    }

    /** Devuelve el resumen del panel según el rol de la cuenta autenticada.
     * @param authentication principal autenticado
     * @return nombre y rol de la cuenta autenticada
     * @throws ResponseStatusException 401 si la cuenta desapareció y 403 sin dashboard
     */
    @GetMapping("/dashboard")
    public Map<String, Object> resumen(Authentication authentication) {
        UserAccount u = users.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "La sesión ya no corresponde a una cuenta activa"));
        String roleKey = u.getRol() == null ? null : Texto.clave(u.getRol().getNombre());
        return Map.of("roleKey", roleKey == null ? "" : roleKey,
                "nombre", u.getFullName(),
                "email", u.getEmail(),
                "rol", u.getRol() == null ? "Sin rol" : u.getRol().getNombre());
    }

    /** Devuelve el catálogo de diagramas y la bitácora documentada para la pantalla de documentación.
     * @return diagramas, secciones de bitácora y tablas disponibles
     */
    @GetMapping("/documentacion")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR_DE_TALLER')")
    public Map<String, Object> documentacion() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("diagramas", List.of(
                Map.of("titulo", "Registro de taller", "archivo", "registro-taller.svg", "fuente", "registro-taller.md"),
                Map.of("titulo", "Registro vehicular", "archivo", "registro-vehicular.svg", "fuente", "registro-vehicular.md"),
                Map.of("titulo", "Arquitectura del sistema", "archivo", "arquitectura-sistema.svg", "fuente", "arquitectura-sistema.md"),
                Map.of("titulo", "Arquitectura fullstack (React + Spring)", "archivo", "arquitectura-fullstack.svg", "fuente", "arquitectura-fullstack.md"),
                Map.of("titulo", "Sistema general y Strategy", "archivo", "diagrama-general.svg", "fuente", "diagrama-general.md")));
        m.put("seccionesBitacora", List.of(
                "Información de fase actual",
                "Estado de tareas",
                "Reglas de negocio numeradas (RN)",
                "Uso del Strategy",
                "Qué queda fuera de alcance",
                "Decisiones tomadas",
                "Pendientes que bloquean el cierre",
                "Criterios de aceptación",
                "Separación backend / frontend"));
        m.put("tablas", tablasDocumentadas());
        return m;
    }

    /** Lista los archivos .md de documentación por tabla si la carpeta existe.
     * @return nombres de archivo ordenados; vacío si la carpeta no está disponible
     */
    private static List<String> tablasDocumentadas() {
        java.nio.file.Path carpeta = java.nio.file.Path.of("tablas");
        if (!java.nio.file.Files.isDirectory(carpeta)) return List.of();
        try (var flujo = java.nio.file.Files.list(carpeta)) {
            return flujo.map(p -> p.getFileName().toString())
                    .filter(nombre -> nombre.endsWith(".md"))
                    .sorted()
                    .toList();
        } catch (java.io.IOException e) {
            return List.of();
        }
    }
}
