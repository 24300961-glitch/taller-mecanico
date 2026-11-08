package mx.tallermecanico.app.sepomex;

import mx.tallermecanico.app.error.NegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** Consulta únicamente el catálogo SEPOMEX almacenado localmente. */
@Service
public class CodigoPostalService {
    private final CodigoPostalRepository repository;

    /** Construye el servicio de consulta postal.
     * @param repository repositorio local del catálogo
     */
    public CodigoPostalService(CodigoPostalRepository repository) { this.repository = repository; }

    /** Devuelve las opciones de colonia, municipio y estado para un CP.
     * @param cp código postal de cinco dígitos
     * @return catálogo local coincidente
     * @throws NegocioException 400 si el formato no es válido y 404 si no hay coincidencias
     */
    @Transactional(readOnly = true)
    public List<Map<String, String>> buscar(String cp) {
        if (cp == null || !cp.matches("\\d{5}")) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "El código postal debe contener exactamente 5 dígitos.",
                    Map.of("errores", Map.of("codigoPostal", "Escribe exactamente 5 dígitos.")));
        }
        List<CodigoPostal> resultados = repository.findByCodigoPostalOrderByEstadoAscMunicipioAscColoniaAsc(cp);
        if (resultados.isEmpty()) throw new NegocioException(HttpStatus.NOT_FOUND,
                "El código postal no se encontró en el catálogo local SEPOMEX. Puedes corregir los campos manualmente.");
        return resultados.stream().map(c -> Map.of("codigoPostal", c.getCodigoPostal(), "colonia", c.getColonia(),
                "municipio", c.getMunicipio(), "estado", c.getEstado())).toList();
    }

    /** Devuelve los estados distintos del catálogo para el combo en cascada.
     * @return nombres de estado ordenados alfabéticamente
     */
    @Transactional(readOnly = true)
    public List<String> estados() {
        return repository.findEstados();
    }

    /** Devuelve los municipios de un estado para el combo en cascada.
     * @param estado nombre de la entidad federativa
     * @return municipios ordenados alfabéticamente
     * @throws NegocioException 400 si el estado viene vacío
     */
    @Transactional(readOnly = true)
    public List<String> municipios(String estado) {
        String e = estado == null ? "" : estado.trim();
        if (e.isEmpty()) throw new NegocioException(HttpStatus.BAD_REQUEST, "El estado es obligatorio.",
                Map.of("errores", Map.of("estado", "Selecciona un estado.")));
        return repository.findMunicipiosByEstado(e);
    }

    /** Devuelve las colonias de un municipio con su CP para el combo en cascada.
     * @param estado nombre de la entidad federativa
     * @param municipio nombre del municipio
     * @return lista de colonia y código postal ordenada alfabéticamente
     * @throws NegocioException 400 si el estado o el municipio vienen vacíos
     */
    @Transactional(readOnly = true)
    public List<Map<String, String>> colonias(String estado, String municipio) {
        String e = estado == null ? "" : estado.trim();
        String m = municipio == null ? "" : municipio.trim();
        if (e.isEmpty() || m.isEmpty()) throw new NegocioException(HttpStatus.BAD_REQUEST,
                "El estado y el municipio son obligatorios.",
                Map.of("errores", Map.of("municipio", "Selecciona estado y municipio.")));
        return repository.findColoniasByEstadoMunicipio(e, m).stream()
                .map(fila -> Map.of("colonia", (String) fila[0], "codigoPostal", (String) fila[1])).toList();
    }
}
