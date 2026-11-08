package mx.tallermecanico.app.sepomex;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

/** Consultas locales sobre el catálogo SEPOMEX. */
public interface CodigoPostalRepository extends JpaRepository<CodigoPostal, Long> {
    /** Busca las colonias ligadas a un código postal.
     * @param codigoPostal CP normalizado
     * @return coincidencias ordenadas por entidad, municipio y colonia
     */
    List<CodigoPostal> findByCodigoPostalOrderByEstadoAscMunicipioAscColoniaAsc(String codigoPostal);

    /** Lista los estados (entidades federativas) distintos del catálogo.
     * @return nombres de estado ordenados alfabéticamente
     */
    @Query("SELECT DISTINCT c.estado FROM CodigoPostal c ORDER BY c.estado")
    List<String> findEstados();

    /** Lista los municipios distintos de un estado.
     * @param estado nombre de la entidad federativa
     * @return municipios ordenados alfabéticamente
     */
    @Query("SELECT DISTINCT c.municipio FROM CodigoPostal c WHERE c.estado = :estado ORDER BY c.municipio")
    List<String> findMunicipiosByEstado(@Param("estado") String estado);

    /** Lista las colonias de un municipio con su código postal.
     * @param estado nombre de la entidad federativa
     * @param municipio nombre del municipio
     * @return filas de colonia y CP ordenadas alfabéticamente
     */
    @Query("SELECT c.colonia, c.codigoPostal FROM CodigoPostal c WHERE c.estado = :estado AND c.municipio = :municipio ORDER BY c.colonia")
    List<Object[]> findColoniasByEstadoMunicipio(@Param("estado") String estado, @Param("municipio") String municipio);
}
