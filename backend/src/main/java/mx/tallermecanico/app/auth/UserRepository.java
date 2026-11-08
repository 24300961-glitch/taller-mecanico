package mx.tallermecanico.app.auth;

import java.util.List;
import java.util.Optional;
import mx.tallermecanico.app.seguridad.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmail(String email);
    boolean existsByEmail(String email);
    List<UserAccount> findAllByOrderByFullNameAsc();
    long countByRol_Id(Long rolId);

    @Query("select count(u) from UserAccount u join u.rol r where u.enabled = true and :permiso member of r.permisos")
    long contarActivosConPermiso(Permiso permiso);
}
