package mx.tallermecanico.app.seguridad;

import mx.tallermecanico.app.audit.AuditService;
import mx.tallermecanico.app.auth.UserRepository;
import mx.tallermecanico.app.common.Texto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RolService {

    private static final Logger log = LoggerFactory.getLogger(RolService.class);

    private final RolRepository roles;
    private final AuditService audit;
    private final UserRepository users;
    private final SeguridadProperties seguridad;

    public RolService(RolRepository roles, AuditService audit, UserRepository users, SeguridadProperties seguridad) {
        this.roles = roles;
        this.audit = audit;
        this.users = users;
        this.seguridad = seguridad;
    }

    @Transactional(readOnly = true)
    public List<Rol> listar() {
        return roles.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Rol obtenerPorNombre(String nombre) {
        String clave = Texto.clave(nombre);
        if (clave == null) {
            throw new IllegalStateException("No se configuró el nombre del rol requerido");
        }
        return roles.findByClave(clave)
                .orElseThrow(() -> new IllegalStateException("El rol configurado '" + nombre + "' no existe en la base de datos"));
    }

    @Transactional
    public Rol crear(String nombre) {
        String limpio = Texto.limpiar(nombre);
        String clave = Texto.clave(limpio);
        if (limpio == null || clave == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del rol es obligatorio");
        }
        if (roles.existsByClave(clave)) {
            log.info("[Roles] Intento de crear rol duplicado: {}", limpio);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un rol con el nombre \"" + limpio + "\"");
        }
        Rol rol;
        try {
            rol = roles.saveAndFlush(new Rol(limpio));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un rol con el nombre \"" + limpio + "\"");
        }
        audit.record("ROL", rol.getId(), "ROL_CREADO", "nombre=" + rol.getNombre());
        return rol;
    }

    @Transactional(readOnly = true)
    public Rol obtener(Long id) {
        return roles.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El rol seleccionado no existe"));
    }

    public boolean esInicial(Rol rol) {
        return clavesIniciales().contains(rol.getClave());
    }

    @Transactional
    public Rol actualizar(Long id, String nombre) {
        Rol rol = obtener(id);
        String limpio = Texto.limpiar(nombre);
        String clave = Texto.clave(limpio);
        if (limpio == null || clave == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del rol es obligatorio");
        }
        if (roles.existsByClaveAndIdNot(clave, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un rol con el nombre \"" + limpio + "\"");
        }
        String anterior = rol.getNombre();
        if (anterior.equals(limpio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay cambios que guardar");
        }
        try {
            rol.renombrar(limpio);
            roles.saveAndFlush(rol);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un rol con el nombre \"" + limpio + "\"");
        }
        audit.record("ROL", rol.getId(), "ROL_ACTUALIZADO", "de=" + anterior + " a=" + rol.getNombre());
        return rol;
    }

    @Transactional
    public void eliminar(Long id) {
        Rol rol = obtener(id);
        if (esInicial(rol) || !rol.isAsignable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El rol \"" + rol.getNombre() + "\" es del sistema y no se puede eliminar");
        }
        long asignados = users.countByRol_Id(id);
        if (asignados > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar el rol \"" + rol.getNombre() + "\" porque está asignado a "
                            + asignados + " usuario(s)");
        }
        roles.delete(rol);
        audit.record("ROL", id, "ROL_ELIMINADO", "nombre=" + rol.getNombre());
    }

    /** Suspende un rol sin borrar su fila (soft-delete); deja de ofrecerse para asignar.
     * @param id identificador del rol
     * @return rol suspendido
     * @throws ResponseStatusException si es del sistema o tiene usuarios asignados
     */
    @Transactional
    public Rol suspender(Long id) {
        Rol rol = obtener(id);
        if (esInicial(rol) || !rol.isAsignable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El rol \"" + rol.getNombre() + "\" es del sistema y no se puede suspender");
        }
        if (!rol.isActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El rol \"" + rol.getNombre() + "\" ya está suspendido");
        }
        long asignados = users.countByRol_Id(id);
        if (asignados > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede suspender el rol \"" + rol.getNombre() + "\" porque está asignado a "
                            + asignados + " usuario(s). Reasígnalos primero.");
        }
        rol.cambiarEstatus(false);
        roles.save(rol);
        audit.record("ROL", id, "ROL_SUSPENDIDO", "nombre=" + rol.getNombre());
        return rol;
    }

    /** Reactiva un rol suspendido para volver a ofrecerlo en asignaciones.
     * @param id identificador del rol
     * @return rol reactivado
     * @throws ResponseStatusException si no existe o ya está activo
     */
    @Transactional
    public Rol reactivar(Long id) {
        Rol rol = obtener(id);
        if (rol.isActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El rol \"" + rol.getNombre() + "\" ya está activo");
        }
        rol.cambiarEstatus(true);
        roles.save(rol);
        audit.record("ROL", id, "ROL_REACTIVADO", "nombre=" + rol.getNombre());
        return rol;
    }

    private Set<String> clavesIniciales() {
        return seguridad.getRolesIniciales().stream()
                .map(r -> Texto.clave(r.getNombre()))
                .filter(c -> c != null)
                .collect(Collectors.toSet());
    }
}
