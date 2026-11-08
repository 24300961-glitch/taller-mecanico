package mx.tallermecanico.app.vehiculos;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Consultas del historial de movimientos de vehículos. */
public interface HistorialVehiculoRepository extends JpaRepository<HistorialVehiculo, Long> {

    /** Busca movimientos por nombre de cliente o placa, del más reciente al más antiguo.
     * @param q texto a buscar en cliente o placa
     * @param tope máximo de resultados
     * @return movimientos ordenados por fecha descendente
     */
    @Query("select h from HistorialVehiculo h where lower(h.clienteNombre) like lower(concat('%', :q, '%'))"
            + " or lower(h.placa) like lower(concat('%', :q, '%')) order by h.createdAt desc")
    List<HistorialVehiculo> buscarPorClienteOPlaca(String q, Pageable tope);

    /** Últimos movimientos registrados en el sistema.
     * @param tope máximo de resultados
     * @return movimientos ordenados por fecha descendente
     */
    @Query("select h from HistorialVehiculo h order by h.createdAt desc")
    List<HistorialVehiculo> recientes(Pageable tope);
}
