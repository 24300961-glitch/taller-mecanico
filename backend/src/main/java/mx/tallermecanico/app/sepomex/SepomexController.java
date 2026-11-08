package mx.tallermecanico.app.sepomex;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Endpoints del catálogo SEPOMEX para selección en cascada (estado → municipio → colonia). */
@RestController
@RequestMapping("/api/sepomex")
public class SepomexController {
    private final CodigoPostalService service;

    /** Construye el controlador SEPOMEX en cascada.
     * @param service consulta del catálogo local
     */
    public SepomexController(CodigoPostalService service) { this.service = service; }

    /** Devuelve los estados del catálogo para el combo inicial.
     * @return lista de nombres de estado
     */
    @GetMapping("/estados")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR_DE_TALLER')")
    public List<String> estados() { return service.estados(); }

    /** Devuelve los municipios de un estado para el segundo combo.
     * @param estado nombre de la entidad federativa
     * @return lista de municipios
     */
    @GetMapping("/estados/{estado}/municipios")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR_DE_TALLER')")
    public List<String> municipios(@PathVariable String estado) { return service.municipios(estado); }

    /** Devuelve las colonias de un municipio con su CP para el tercer combo.
     * @param estado nombre de la entidad federativa
     * @param municipio nombre del municipio
     * @return lista de colonia y código postal
     */
    @GetMapping("/estados/{estado}/municipios/{municipio}/colonias")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR_DE_TALLER')")
    public List<Map<String, String>> colonias(@PathVariable String estado, @PathVariable String municipio) {
        return service.colonias(estado, municipio);
    }
}
