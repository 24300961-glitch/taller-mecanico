package mx.tallermecanico.app.clientes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import mx.tallermecanico.app.taller.Taller;
import mx.tallermecanico.app.error.NegocioException;
import mx.tallermecanico.app.vehiculos.VehiculoController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService service;

    /** Construye el controlador de clientes.
     * @param service reglas del módulo de clientes
     */
    public ClienteController(ClienteService service) {
        this.service = service;
    }

    /** Solicitud validada para desactivar un cliente.
     * @param motivo motivo informado por el usuario
     */
    public record DesactivarRequest(
            @NotBlank(message = "Indica el motivo de la desactivación")
            @Size(min = 5, max = 250, message = "El motivo debe tener entre 5 y 250 caracteres") String motivo) {}

    @GetMapping("/alta")
    @PreAuthorize("hasAuthority('CLIENTES_REGISTRAR')")
    /** Comprueba que haya talleres activos y devuelve opciones para asociar.
     * @return lista de talleres activos y mensaje de formulario
     * @throws NegocioException 409 si no existe un taller activo
     */
    public Map<String, Object> abrirAlta() {
        List<Taller> activos = service.talleresActivos();
        if (activos.isEmpty()) throw new mx.tallermecanico.app.error.NegocioException(HttpStatus.CONFLICT,
                "No hay talleres activos. El Administrador debe registrar o activar un taller antes de crear clientes.");
        return Map.of("permitido", true, "talleres", activos.stream().map(t -> Map.of("id", t.getId(), "nombre", t.getNombre())),
                "message", "Selecciona el taller o talleres para asociar al cliente.");
    }

    @PostMapping("/coincidencias")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR_DE_TALLER','ROLE_RECEPCIONISTA')")
    /** Busca coincidencia exacta por email para bloquear un alta duplicada.
     * @param r datos normalizados del formulario
     * @param excluirId id omitido al editar
     * @return coincidencia de email o lista vacía
     */
    public Map<String, Object> coincidencias(@Valid @RequestBody ClienteRequest r,
                                             @RequestParam(required = false) Long excluirId) {
        List<ClienteService.Coincidencia> lista = service.buscarCoincidencias(service.normalizar(r), excluirId);
        boolean bloqueante = lista.stream().anyMatch(ClienteService.Coincidencia::bloqueante);
        String mensaje = lista.isEmpty() ? "No se encontraron clientes con ese correo electrónico."
                : "Ya existe un cliente con el correo electrónico " + lista.get(0).email() + ". Elimina o reactiva el registro anterior.";
        return Map.of("coincidencias", lista, "bloqueante", bloqueante, "message", mensaje);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENTES_REGISTRAR')")
    /** Crea un cliente nuevo por email y asocia talleres seleccionados.
     * @param r formulario completo con uno o más talleres activos
     * @return HTTP 201 y datos del cliente creado
     * @throws NegocioException 409 si ya existe el correo; 400 si falta un taller o hay datos inválidos
     */
    public ResponseEntity<Map<String, Object>> registrar(@Valid @RequestBody ClienteRequest r) {
        Cliente c = service.registrar(r);
        Map<String, Object> m = vista(c);
        m.put("talleres", c.getTalleres().stream().map(t -> Map.of("id", t.getId(), "nombre", t.getNombre(), "activo", t.isActivo())).toList());
        m.put("message", "Cliente " + c.getNombreCompleto() + " registrado con ID " + c.getId() + ".");
        return ResponseEntity.status(HttpStatus.CREATED).body(m);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR_DE_TALLER','ROLE_RECEPCIONISTA')")
    /** Consulta clientes para usuarios con permiso de consulta.
     * @param q búsqueda opcional
     * @param incluirInactivos indica si incluir inactivos
     * @return lista de clientes
     */
    public List<Map<String, Object>> consultar(@RequestParam(required = false) String q,
                                               @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return service.consultar(q, incluirInactivos).stream().map(ClienteController::vista).toList();
    }

    @GetMapping("/pagina")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR_DE_TALLER','ROLE_RECEPCIONISTA')")
    /** Pagina clientes con totales para la administración con paginador.
     * @param q búsqueda opcional
     * @param incluirInactivos indica si incluir inactivos
     * @param page número de página base cero
     * @param size filas por página
     * @return contenido y totales de la página
     */
    public Map<String, Object> pagina(@RequestParam(required = false) String q,
                                      @RequestParam(defaultValue = "false") boolean incluirInactivos,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Page<Cliente> p = service.consultarPagina(q, incluirInactivos, page, size);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("contenido", p.getContent().stream().map(ClienteController::vista).toList());
        m.put("pagina", p.getNumber());
        m.put("tamano", p.getSize());
        m.put("totalElementos", p.getTotalElements());
        m.put("totalPaginas", p.getTotalPages());
        return m;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR_DE_TALLER','ROLE_RECEPCIONISTA')")
    /** Devuelve un cliente con sus talleres asociados y vehículos.
     * @param id identificador del cliente
     * @return datos relacionados del cliente
     * @throws NegocioException 404 si el cliente no existe
     */
    public Map<String, Object> obtener(@PathVariable Long id) {
        Cliente cliente = service.obtenerConTalleres(id);
        Map<String, Object> m = vista(cliente);
        m.put("talleres", cliente.getTalleres().stream().map(t -> Map.of("id", t.getId(), "nombre", t.getNombre(), "activo", t.isActivo())).toList());
        m.put("vehiculos", service.vehiculosDe(id).stream().map(VehiculoController::vista).toList());
        return m;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR_DE_TALLER','ROLE_RECEPCIONISTA')")
    /** Actualiza los datos de una persona sin cambiar sus asociaciones a talleres.
     * @param id cliente existente
     * @param r formulario de datos
     * @return datos actualizados
     */
    public Map<String, Object> actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest r) {
        Cliente c = service.actualizar(id, r);
        Map<String, Object> m = vista(c);
        m.put("message", "Datos del cliente actualizados correctamente.");
        return m;
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR_DE_TALLER','ROLE_RECEPCIONISTA')")
    /** Desactiva un cliente y conserva sus relaciones.
     * @param id cliente existente
     * @param r motivo validado
     * @return cliente desactivado
     */
    public Map<String, Object> desactivar(@PathVariable Long id, @Valid @RequestBody DesactivarRequest r) {
        Cliente c = service.desactivar(id, r.motivo());
        Map<String, Object> m = vista(c);
        m.put("message", "Cliente desactivado. Ya no aparecerá en las búsquedas de clientes activos.");
        return m;
    }

    @PatchMapping("/{id}/reactivar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR_DE_TALLER','ROLE_RECEPCIONISTA')")
    /** Reactiva un cliente.
     * @param id cliente existente
     * @return cliente reactivado
     */
    public Map<String, Object> reactivar(@PathVariable Long id) {
        Cliente c = service.reactivar(id);
        Map<String, Object> m = vista(c);
        m.put("message", "Cliente reactivado correctamente.");
        return m;
    }

    /** Convierte el cliente a un DTO seguro.
     * @param c entidad consultada
     * @return mapa de salida
     */
    static Map<String, Object> vista(Cliente c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("nombre", c.getNombre());
        m.put("apellidos", c.getApellidos());
        m.put("apellidoPaterno", c.getApellidoPaterno());
        m.put("apellidoMaterno", c.getApellidoMaterno());
        m.put("nombreCompleto", c.getNombreCompleto());
        m.put("telefono", c.getTelefono());
        m.put("telefonoAlterno", c.getTelefonoAlterno());
        m.put("telefonoPersonal", c.getTelefonoPersonal());
        m.put("telefonoTrabajo", c.getTelefonoTrabajo());
        m.put("nombreContacto", c.getNombreContacto());
        m.put("telefonoContacto", c.getTelefonoContacto());
        m.put("email", c.getEmail());
        m.put("rfc", c.getRfc());
        m.put("direccion", c.getDireccion());
        m.put("calle", c.getCalle());
        m.put("numero", c.getNumero());
        m.put("colonia", c.getColonia());
        m.put("codigoPostal", c.getCodigoPostal());
        m.put("municipio", c.getMunicipio());
        m.put("estado", c.getEstado());
        m.put("activo", c.isActivo());
        m.put("motivoDesactivacion", c.getMotivoDesactivacion());
        m.put("desactivadoPor", c.getDesactivadoPor());
        m.put("desactivadoEn", c.getDesactivadoEn() == null ? null : c.getDesactivadoEn().toString());
        m.put("creadoPor", c.getCreatedBy());
        m.put("creadoEn", c.getCreatedAt() == null ? null : c.getCreatedAt().toString());
        m.put("actualizadoPor", c.getUpdatedBy());
        m.put("actualizadoEn", c.getUpdatedAt() == null ? null : c.getUpdatedAt().toString());
        return m;
    }
}
