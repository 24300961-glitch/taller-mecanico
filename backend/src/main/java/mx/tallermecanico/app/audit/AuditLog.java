package mx.tallermecanico.app.audit;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_entity", columnList = "entityType,entityId"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_performedBy", columnList = "performedBy"),
        @Index(name = "idx_audit_createdAt", columnList = "createdAt")
})
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String entityType;

    private Long entityId;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(length = 190)
    private String performedBy;

    @Column(length = 190)
    private String targetUser;

    @Column(length = 2000)
    private String detail;

    @Column(length = 45)
    private String remoteIp;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLog() {}

    public AuditLog(String entityType, Long entityId, String action, String performedBy,
                    String targetUser, String detail, String remoteIp) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.performedBy = performedBy;
        this.targetUser = targetUser;
        this.detail = detail;
        this.remoteIp = remoteIp;
    }

    public static AuditLog of(String entityType, Long entityId, String action,
                              String performedBy, String detail, String remoteIp) {
        return new AuditLog(entityType, entityId, action, performedBy, null, detail, remoteIp);
    }

    public Long getId() { return id; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getAction() { return action; }
    public String getPerformedBy() { return performedBy; }
    public String getTargetUser() { return targetUser; }
    public String getDetail() { return detail; }
    public String getRemoteIp() { return remoteIp; }
    public Instant getCreatedAt() { return createdAt; }
}
