package mx.tallermecanico.app.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByClave(String clave);
    boolean existsByClave(String clave);
    boolean existsByClaveAndIdNot(String clave, Long id);
    List<Rol> findAllByOrderByNombreAsc();
}
