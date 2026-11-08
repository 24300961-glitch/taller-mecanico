package mx.tallermecanico.app.seguridad;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/roles")
public class RolController {

    private final RolService roles;

    public RolController(RolService roles) {
        this.roles = roles;
    }

    public record CrearRolRequest(
            @NotBlank(message = "El nombre del rol es obligatorio")
            @Size(min = 3, max = 60, message = "El nombre del rol debe tener entre 3 y 60 caracteres")
            @Pattern(regexp = "^[\\p{L}0-9][\\p{L}0-9 _-]*$", message = "El nombre del rol solo puede contener letras, números, espacios, guiones y guiones bajos")
            String nombre) {}

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLES_CREAR','ROLES_ASIGNAR')")
    public List<Map<String, Object>> listar() {
        return roles.listar().stream().map(r -> vista(r, roles.esInicial(r))).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLES_CREAR')")
    public Map<String, Object> crear(@Valid @RequestBody CrearRolRequest req) {
        Rol rol = roles.crear(req.nombre());
        Map<String, Object> r = vista(rol, roles.esInicial(rol));
        r.put("message", "Rol \"" + rol.getNombre() + "\" creado correctamente.");
        return r;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLES_CREAR')")
    public Map<String, Object> actualizar(@PathVariable Long id, @Valid @RequestBody CrearRolRequest req) {
        Rol rol = roles.actualizar(id, req.nombre());
        Map<String, Object> r = vista(rol, roles.esInicial(rol));
        r.put("message", "Rol \"" + rol.getNombre() + "\" actualizado correctamente.");
        return r;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLES_CREAR')")
    public Map<String, Object> eliminar(@PathVariable Long id) {
        Rol rol = roles.obtener(id);
        String nombre = rol.getNombre();
        roles.eliminar(id);
        return Map.of("message", "Rol \"" + nombre + "\" eliminado correctamente.");
    }

    @PatchMapping("/{id}/suspender")
    @PreAuthorize("hasAuthority('ROLES_CREAR')")
    /** Suspende un rol sin borrar su fila; deja de ofrecerse para asignar.
     * @param id identificador del rol
     * @return rol suspendido
     */
    public Map<String, Object> suspender(@PathVariable Long id) {
        Rol rol = roles.suspender(id);
        Map<String, Object> r = vista(rol, roles.esInicial(rol));
        r.put("message", "Rol \"" + rol.getNombre() + "\" suspendido. Se conserva en la base de datos y ya no se ofrece para asignar.");
        return r;
    }

    @PatchMapping("/{id}/reactivar")
    @PreAuthorize("hasAuthority('ROLES_CREAR')")
    /** Reactiva un rol suspendido para volver a ofrecerlo.
     * @param id identificador del rol
     * @return rol reactivado
     */
    public Map<String, Object> reactivar(@PathVariable Long id) {
        Rol rol = roles.reactivar(id);
        Map<String, Object> r = vista(rol, roles.esInicial(rol));
        r.put("message", "Rol \"" + rol.getNombre() + "\" reactivado correctamente.");
        return r;
    }

    static Map<String, Object> vista(Rol r) {
        return vista(r, false);
    }

    static Map<String, Object> vista(Rol r, boolean inicial) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("nombre", r.getNombre());
        m.put("asignable", r.isAsignable());
        m.put("activo", r.isActivo());
        m.put("inicial", inicial);
        m.put("permisos", r.getPermisos().stream().map(Permiso::getDescripcion).sorted().toList());
        m.put("creadoPor", r.getCreatedBy());
        m.put("creadoEn", r.getCreatedAt() == null ? null : r.getCreatedAt().toString());
        return m;
    }
}
