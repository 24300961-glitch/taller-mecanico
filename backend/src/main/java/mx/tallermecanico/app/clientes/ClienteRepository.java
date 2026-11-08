package mx.tallermecanico.app.clientes;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /** Recupera un cliente por correo ignorando mayúsculas y minúsculas.
     * @param email correo normalizado
     * @return cliente existente si el correo ya está registrado
     */
    Optional<Cliente> findFirstByEmailIgnoreCase(String email);

    /** Carga un cliente y sus talleres sin lazy loading fuera de la transacción.
     * @param id identificador del cliente
     * @return cliente con sus asociaciones de taller
     */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "talleres")
    Optional<Cliente> findWithTalleresById(Long id);

    @Query("""
            select c from Cliente c
            where (:excluirId is null or c.id <> :excluirId)
              and ( (:email is not null and lower(c.email) = :email)
                 or (:rfc is not null and c.rfc = :rfc)
                 or (:nombre is not null and c.nombreNormalizado = :nombre)
                 or (:tel is not null and (c.telefonoNormalizado = :tel or c.telefonoAlternoNormalizado = :tel
                     or c.telefonoPersonalNormalizado = :tel or c.telefonoTrabajoNormalizado = :tel
                     or c.telefonoContactoNormalizado = :tel))
                 or (:telAlt is not null and (c.telefonoNormalizado = :telAlt or c.telefonoAlternoNormalizado = :telAlt
                     or c.telefonoPersonalNormalizado = :telAlt or c.telefonoTrabajoNormalizado = :telAlt
                     or c.telefonoContactoNormalizado = :telAlt))
                 or (:telPer is not null and (c.telefonoNormalizado = :telPer or c.telefonoAlternoNormalizado = :telPer
                     or c.telefonoPersonalNormalizado = :telPer or c.telefonoTrabajoNormalizado = :telPer
                     or c.telefonoContactoNormalizado = :telPer))
                 or (:telTra is not null and (c.telefonoNormalizado = :telTra or c.telefonoAlternoNormalizado = :telTra
                     or c.telefonoPersonalNormalizado = :telTra or c.telefonoTrabajoNormalizado = :telTra
                     or c.telefonoContactoNormalizado = :telTra))
                 or (:telCon is not null and (c.telefonoNormalizado = :telCon or c.telefonoAlternoNormalizado = :telCon
                     or c.telefonoPersonalNormalizado = :telCon or c.telefonoTrabajoNormalizado = :telCon
                     or c.telefonoContactoNormalizado = :telCon)) )
            order by c.activo desc, c.nombreNormalizado
            """)
    List<Cliente> buscarCoincidencias(@Param("excluirId") Long excluirId,
                                      @Param("email") String email,
                                      @Param("rfc") String rfc,
                                      @Param("nombre") String nombre,
                                      @Param("tel") String tel,
                                      @Param("telAlt") String telAlt,
                                      @Param("telPer") String telPer,
                                      @Param("telTra") String telTra,
                                      @Param("telCon") String telCon);

    List<Cliente> findByActivoTrueOrderByNombreNormalizadoAsc();

    @Query("""
            select count(c) from Cliente c
            where (:incluirInactivos = true or c.activo = true)
              and ( :texto is null
                 or (:id is not null and c.id = :id)
                 or c.nombreNormalizado like :texto
                 or lower(c.email) like :correo
                 or (:digitos is not null and (c.telefonoNormalizado like :digitos or c.telefonoAlternoNormalizado like :digitos
                     or c.telefonoPersonalNormalizado like :digitos or c.telefonoTrabajoNormalizado like :digitos
                     or c.telefonoContactoNormalizado like :digitos)) )
            """)
    long contar(@Param("texto") String texto,
                @Param("correo") String correo,
                @Param("digitos") String digitos,
                @Param("id") Long id,
                @Param("incluirInactivos") boolean incluirInactivos);

    @Query("""
            select c from Cliente c
            where (:incluirInactivos = true or c.activo = true)
              and ( :texto is null
                 or (:id is not null and c.id = :id)
                 or c.nombreNormalizado like :texto
                 or lower(c.email) like :correo
                 or (:digitos is not null and (c.telefonoNormalizado like :digitos or c.telefonoAlternoNormalizado like :digitos
                     or c.telefonoPersonalNormalizado like :digitos or c.telefonoTrabajoNormalizado like :digitos
                     or c.telefonoContactoNormalizado like :digitos)) )
            order by c.activo desc, c.nombreNormalizado
            """)
    List<Cliente> consultar(@Param("texto") String texto,
                            @Param("correo") String correo,
                            @Param("digitos") String digitos,
                            @Param("id") Long id,
                            @Param("incluirInactivos") boolean incluirInactivos,
                            Pageable pageable);
}
