package mx.tallermecanico.app.taller;

import jakarta.validation.Valid;
import mx.tallermecanico.app.error.NegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Rutas de administración de talleres; no usa Strategy. */
@RestController
@RequestMapping("/api/talleres")
public class TallerController {
    private final TallerService service;

    /** Construye el controlador de talleres.
     * @param service reglas de taller
     */
    public TallerController(TallerService service) {
        this.service = service;
    }

    /** Lista talleres para administración.
     * @param incluirInactivos indica si se incluyen talleres inactivos
     * @return representación de talleres
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR_DE_TALLER')")
    public List<Map<String, Object>> consultar(@RequestParam(defaultValue = "true") boolean incluirInactivos) {
        return service.consultar(incluirInactivos).stream().map(TallerController::vista).toList();
    }

    /** Devuelve los talleres activos disponibles para asociaciones.
     * @return opciones de taller para altas autorizadas
     */
    @GetMapping("/activos")
    @PreAuthorize("hasAnyAuthority('CLIENTES_REGISTRAR','VEHICULOS_REGISTRAR','VEHICULOS_EDITAR')")
    public List<Map<String, Object>> activos() {
        return service.consultar(false).stream()
                .map(t -> Map.<String, Object>of("id", t.getId(), "nombre", t.getNombre()))
                .toList();
    }

    /** Registra un taller con foto opcional.
     * @param request campos del formulario validados
     * @param foto imagen opcional
     * @return resultado y datos del taller creado
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR_DE_TALLER')")
    public Map<String, Object> registrar(@Valid @ModelAttribute TallerRequest request,
                                         @RequestParam(value = "foto", required = false) MultipartFile foto) {
        Taller taller = service.registrar(request, foto);
        Map<String, Object> result = vista(taller);
        result.put("insertado", true);
        result.put("message", "Taller creado correctamente.");
        return result;
    }

    /** Bloquea el borrado físico del taller.
     * @param id taller solicitado
     * @return nunca retorna; responde 409 con indicación de desactivar
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR_DE_TALLER')")
    public Map<String, Object> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return Map.of("insertado", false);
    }

    /** Actualiza los datos y la foto opcional de un taller existente.
     * @param id identificador del taller
     * @param request campos del formulario validados
     * @param foto imagen nueva opcional que reemplaza la anterior
     * @return resultado y datos del taller actualizado
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR_DE_TALLER')")
    public Map<String, Object> actualizar(@PathVariable Long id, @Valid @ModelAttribute TallerRequest request,
                                          @RequestParam(value = "foto", required = false) MultipartFile foto) {
        Map<String, Object> result = vista(service.actualizar(id, request, foto));
        result.put("insertado", false);
        result.put("message", "Taller actualizado correctamente.");
        return result;
    }

    /** Desactiva el taller conservando relaciones y vehículos asignados.
     * @param id taller solicitado
     * @return taller actualizado
     */
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR_DE_TALLER')")
    public Map<String, Object> desactivar(@PathVariable Long id) {
        Map<String, Object> result = vista(service.desactivar(id));
        result.put("message", "Taller desactivado; sus clientes y vehículos siguen visibles.");
        return result;
    }

    /** Reactiva un taller existente.
     * @param id taller solicitado
     * @return taller actualizado
     */
    @PatchMapping("/{id}/reactivar")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR_DE_TALLER')")
    public Map<String, Object> reactivar(@PathVariable Long id) {
        Map<String, Object> result = vista(service.reactivar(id));
        result.put("message", "Taller reactivado correctamente.");
        return result;
    }

    /** Construye la respuesta JSON del taller.
     * @param taller taller persistido
     * @return mapa con todos los campos del registro
     */
    public static Map<String, Object> vista(Taller taller) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", taller.getId()); m.put("nombre", taller.getNombre()); m.put("rfc", taller.getRfc());
        m.put("email", taller.getEmail()); m.put("telefono", taller.getTelefono());
        m.put("fotoPath", taller.getFotoPath());
        m.put("calle", taller.getCalle()); m.put("numero", taller.getNumero()); m.put("colonia", taller.getColonia());
        m.put("codigoPostal", taller.getCodigoPostal()); m.put("municipio", taller.getMunicipio());
        m.put("estado", taller.getEstado()); m.put("activo", taller.isActivo());
        return m;
    }
}
