package mx.tallermecanico.app.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();

    List<AuditLog> findByPerformedByOrderByCreatedAtDesc(String performedBy);

    @Query("select a from AuditLog a where a.entityType = ?1 and a.entityId = ?2 order by a.createdAt desc")
    List<AuditLog> findForEntity(String entityType, Long entityId);

    long countByCreatedAtAfter(Instant from);
}
