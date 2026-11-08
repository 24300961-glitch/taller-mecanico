package mx.tallermecanico.app.taller;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface TallerRepository extends JpaRepository<Taller, Long> {
    Optional<Taller> findFirstByOrderByIdAsc();

    /** Busca talleres por RFC exacto.
     * @param rfc RFC normalizado a mayúsculas
     * @return taller coincidente si existe
     */
    Optional<Taller> findFirstByRfc(String rfc);

    /** Busca talleres con nombre y domicilio normalizados coincidentes.
     * @param nombre nombre en minúsculas
     * @param calle calle en minúsculas
     * @param numero número del domicilio en minúsculas
     * @param colonia colonia en minúsculas
     * @param codigoPostal código postal
     * @param municipio municipio en minúsculas
     * @param estado estado en minúsculas
     * @return coincidencias existentes
     */
    List<Taller> findByNombreAndCalleAndNumeroAndColoniaAndCodigoPostalAndMunicipioAndEstado(
            String nombre, String calle, String numero, String colonia, String codigoPostal, String municipio, String estado);

    /** Lista talleres opcionalmente filtrados por estatus.
     * @param activo true para activos, false para inactivos
     * @return talleres ordenados por nombre
     */
    List<Taller> findAllByActivoOrderByNombreAsc(boolean activo);
}
