package mx.tallermecanico.app.vehiculos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import mx.tallermecanico.app.clientes.Cliente;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoService service;

    /** Construye el controlador de vehículos.
     * @param service reglas del módulo de vehículos
     */
    public VehiculoController(VehiculoService service) {
        this.service = service;
    }

    /** Solicitud para desactivar un vehículo.
     * @param motivo motivo de desactivación
     */
    public record DesactivarRequest(
            @NotBlank(message = "Indica el motivo de la desactivación")
            @Size(min = 5, max = 250, message = "El motivo debe tener entre 5 y 250 caracteres") String motivo) {}

    @PostMapping("/coincidencias")
    @PreAuthorize("hasAnyAuthority('VEHICULOS_REGISTRAR','VEHICULOS_EDITAR')")
    /** Comprueba placa y VIN antes de registrar o actualizar.
     * @param r formulario validado
     * @param excluirId vehículo omitido al editar
     * @return coincidencias encontradas
     */
    public Map<String, Object> coincidencias(@Valid @RequestBody VehiculoRequest r,
                                             @RequestParam(required = false) Long excluirId) {
        List<VehiculoService.Coincidencia> lista = service.buscarCoincidencias(service.normalizar(r), excluirId);
        return Map.of("coincidencias", lista, "bloqueante", !lista.isEmpty(),
                "message", lista.isEmpty() ? "La placa y el VIN están disponibles." : "Ya existe un vehículo con la misma placa o VIN.");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('VEHICULOS_REGISTRAR')")
    /** Registra el vehículo y exige un taller activo específico.
     * @param r datos del vehículo, propietario y taller
     * @return vehículo creado
     */
    public Map<String, Object> registrar(@Valid @RequestBody VehiculoRequest r) {
        Vehiculo v = service.registrar(r);
        Map<String, Object> m = vista(v);
        m.put("message", "Vehículo con placa " + v.getPlaca() + " registrado correctamente.");
        return m;
    }

    @GetMapping("/catalogo")
    @PreAuthorize("hasAnyAuthority('VEHICULOS_CONSULTAR','VEHICULOS_REGISTRAR','VEHICULOS_EDITAR')")
    /** Devuelve el catálogo vehicular existente.
     * @return marcas y modelos
     */
    public Map<String, Object> catalogo() {
        return Map.of("marcas", CatalogoVehiculos.marcas());
    }

    @GetMapping("/propietarios")
    @PreAuthorize("hasAnyAuthority('VEHICULOS_REGISTRAR','VEHICULOS_EDITAR')")
    /** Lista clientes activos seleccionables como propietarios.
     * @return identificador y nombre de cada cliente
     */
    public List<Map<String, Object>> propietarios() {
        return service.propietariosActivos().stream().map(VehiculoController::vistaPropietario).toList();
    }

    /** Consulta únicamente vehículos de la cuenta Cliente autenticada.
     * @param principal principal autenticado por Spring Security
     * @return vehículos propios con su taller específico
     */
    @GetMapping("/mis")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public List<Map<String, Object>> misVehiculos(java.security.Principal principal) {
        return service.misVehiculos(principal.getName()).stream().map(VehiculoController::vista).toList();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VEHICULOS_CONSULTAR')")
    /** Busca vehículos para administración y recepción.
     * @param q texto opcional de búsqueda
     * @param incluirInactivos indica si deben incluirse inactivos
     * @return vehículos con propietario y taller asignado
     */
    public List<Map<String, Object>> consultar(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) Long clienteId,
                                               @RequestParam(required = false) String placa,
                                               @RequestParam(required = false) String vin,
                                               @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return service.consultar(q, clienteId, placa, vin, incluirInactivos).stream().map(VehiculoController::vista).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VEHICULOS_CONSULTAR')")
    /** Obtiene un vehículo por ID.
     * @param id identificador solicitado
     * @return vehículo encontrado
     */
    public Map<String, Object> obtener(@PathVariable Long id) {
        return vista(service.obtener(id));
    }

    @GetMapping("/historial")
    @PreAuthorize("hasAuthority('VEHICULOS_CONSULTAR')")
    /** Consulta el historial de movimientos por nombre de cliente o placa.
     * @param q texto de búsqueda; vacío devuelve los movimientos recientes
     * @return movimientos del más reciente al más antiguo
     */
    public List<Map<String, Object>> historial(@RequestParam(required = false) String q) {
        return service.historial(q).stream().map(VehiculoController::vistaHistorial).toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('VEHICULOS_EDITAR')")
    /** Actualiza un vehículo y su taller cuando cambia su ubicación.
     * @param id identificador del vehículo
     * @param r datos nuevos y taller seleccionado
     * @return entidad actualizada
     */
    public Map<String, Object> actualizar(@PathVariable Long id, @Valid @RequestBody VehiculoRequest r) {
        Map<String, Object> m = vista(service.actualizar(id, r));
        m.put("message", "Datos del vehículo actualizados correctamente.");
        return m;
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('VEHICULOS_DESACTIVAR')")
    /** Desactiva un vehículo sin borrar sus relaciones.
     * @param id identificador
     * @param r motivo validado
     * @return vehículo desactivado
     */
    public Map<String, Object> desactivar(@PathVariable Long id, @Valid @RequestBody DesactivarRequest r) {
        Map<String, Object> m = vista(service.desactivar(id, r.motivo()));
        m.put("message", "Vehículo desactivado.");
        return m;
    }

    @PatchMapping("/{id}/reactivar")
    @PreAuthorize("hasAuthority('VEHICULOS_DESACTIVAR')")
    /** Reactiva un vehículo.
     * @param id identificador
     * @return vehículo reactivado
     */
    public Map<String, Object> reactivar(@PathVariable Long id) {
        Map<String, Object> m = vista(service.reactivar(id));
        m.put("message", "Vehículo reactivado correctamente.");
        return m;
    }

    /** Construye el DTO de propietario para el selector.
     * @param c cliente existente
     * @return ID y nombre visible
     */
    static Map<String, Object> vistaPropietario(Cliente c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("nombre", c.getNombreCompleto());
        return m;
    }

    /** Construye el DTO de un movimiento del historial.
     * @param h movimiento consultado
     * @return mapa con vehículo, cliente, taller, fecha y detalle
     */
    public static Map<String, Object> vistaHistorial(HistorialVehiculo h) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", h.getId());
        m.put("vehiculoId", h.getVehiculo().getId());
        m.put("placa", h.getPlaca());
        m.put("cliente", h.getClienteNombre());
        m.put("taller", h.getTallerNombre());
        m.put("tipo", h.getTipo().name());
        m.put("detalle", h.getDetalle());
        m.put("fecha", h.getCreatedAt() == null ? null : h.getCreatedAt().toString());
        m.put("por", h.getCreatedBy());
        return m;
    }

    /** Construye el DTO del vehículo para vistas y APIs.
     * @param v vehículo consultado
     * @return mapa con propietario y taller asignado
     */
    public static Map<String, Object> vista(Vehiculo v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", v.getId());
        m.put("clienteId", v.getCliente().getId());
        m.put("cliente", v.getCliente().getNombreCompleto());
        m.put("tallerId", v.getTaller() == null ? null : v.getTaller().getId());
        m.put("taller", v.getTaller() == null ? null : v.getTaller().getNombre());
        m.put("tallerActivo", v.getTaller() != null && v.getTaller().isActivo());
        m.put("marca", v.getMarca());
        m.put("modelo", v.getModelo());
        m.put("anio", v.getAnio());
        m.put("placa", v.getPlaca());
        m.put("vin", v.getVin());
        m.put("color", v.getColor());
        m.put("version", v.getVersion());
        m.put("kilometraje", v.getKilometraje());
        m.put("fotos", v.getFotos());
        m.put("activo", v.isActivo());
        m.put("motivoDesactivacion", v.getMotivoDesactivacion());
        m.put("creadoPor", v.getCreatedBy());
        m.put("creadoEn", v.getCreatedAt() == null ? null : v.getCreatedAt().toString());
        return m;
    }
}
