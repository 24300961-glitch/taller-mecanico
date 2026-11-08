package mx.tallermecanico.app.sepomex;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Endpoint para autocompletar direcciones desde el catálogo SEPOMEX local. */
@RestController
@RequestMapping({"/api/codigos-postales", "/api/sepomex/cp"})
public class CodigoPostalController {
    private final CodigoPostalService service;

    /** Construye el controlador postal.
     * @param service consulta del catálogo local
     */
    public CodigoPostalController(CodigoPostalService service) { this.service = service; }

    /** Busca asentamientos disponibles por CP.
     * @param cp código postal de cinco dígitos
     * @return lista de colonia, municipio y estado
     */
    @GetMapping("/{cp}")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR_DE_TALLER')")
    public List<Map<String, String>> buscar(@PathVariable String cp) { return service.buscar(cp); }
}
