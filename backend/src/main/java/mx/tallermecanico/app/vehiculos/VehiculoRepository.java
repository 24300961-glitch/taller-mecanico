package mx.tallermecanico.app.vehiculos;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    /** Consulta vehículos asignados a talleres con los datos del propietario.
     * @param email correo de la cuenta Cliente
     * @return vehículos del cliente autenticado, incluidos los heredados sin taller
     */
    @Query("select v from Vehiculo v join fetch v.cliente c left join fetch v.taller where lower(c.email) = lower(:email) order by v.activo desc, v.placa")
    List<Vehiculo> findByClienteEmail(@Param("email") String email);

    @Query("select v from Vehiculo v join fetch v.cliente c where c.id = :clienteId order by v.activo desc, v.placa")
    List<Vehiculo> findByClienteIdOrderByActivoDescPlacaAsc(@Param("clienteId") Long clienteId);

    @Query("""
            select v from Vehiculo v join fetch v.cliente
            where (:excluirId is null or v.id <> :excluirId)
              and ( v.placa = :placa or (:vin is not null and v.vin = :vin) )
            """)
    List<Vehiculo> buscarCoincidencias(@Param("excluirId") Long excluirId,
                                       @Param("placa") String placa,
                                       @Param("vin") String vin);

    @Query("""
            select v from Vehiculo v join fetch v.cliente c
            where (:incluirInactivos = true or v.activo = true)
              and (:clienteId is null or c.id = :clienteId)
              and (:placa is null or v.placa like :placa)
              and (:vin is null or v.vin like :vin)
              and ( :texto is null
                 or (:id is not null and v.id = :id)
                 or lower(v.placa) like :texto
                 or lower(v.vin) like :texto
                 or v.vin like :clave
                 or lower(v.marca) like :texto
                 or lower(v.modelo) like :texto
                 or c.nombreNormalizado like :texto )
            order by v.activo desc, v.placa
            """)
    List<Vehiculo> consultar(@Param("texto") String texto,
                             @Param("clave") String clave,
                             @Param("id") Long id,
                             @Param("clienteId") Long clienteId,
                             @Param("placa") String placa,
                             @Param("vin") String vin,
                             @Param("incluirInactivos") boolean incluirInactivos,
                             Pageable pageable);
}
