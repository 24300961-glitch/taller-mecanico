package mx.tallermecanico.app.audit;

import jakarta.servlet.http.HttpServletRequest;
import mx.tallermecanico.app.seguridad.SeguridadProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;
    private final HttpServletRequest request;
    private final SeguridadProperties props;
    private final TransactionTemplate nuevaTransaccion;

    public AuditService(AuditLogRepository repository, HttpServletRequest request, SeguridadProperties props,
                        PlatformTransactionManager txManager) {
        this.repository = repository;
        this.request = request;
        this.props = props;
        this.nuevaTransaccion = new TransactionTemplate(txManager);
        this.nuevaTransaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void record(String entityType, Long entityId, String action, String detail) {
        record(entityType, entityId, action, null, detail);
    }

    public void record(String entityType, Long entityId, String action, String targetUser, String detail) {
        String by = currentUser();
        String ip = clientIp();
        String safeDetail = detail == null ? null : (detail.length() > 2000 ? detail.substring(0, 2000) : detail);
        try {
            AuditLog entrada = new AuditLog(entityType, entityId, action, by, targetUser, safeDetail, ip);
            nuevaTransaccion.executeWithoutResult(s -> repository.save(entrada));
        } catch (Exception ex) {
            log.error("[Audit] No se pudo persistir auditoria: {} {} {} por {}", entityType, entityId, action, by, ex);
        }
        log.info("[Audit] {}:{} {} | by={} | target={} | ip={} | {}",
                entityType, entityId, action, by, targetUser == null ? "" : targetUser, ip, safeDetail == null ? "" : safeDetail);
    }

    public void recordLogin(String email, boolean success, String detail) {
        record("USER_ACCOUNT", null, success ? "LOGIN_OK" : "LOGIN_FAIL", email, detail);
    }

    public void recordPasswordChange(String email, boolean self, String detail) {
        record("USER_ACCOUNT", null, self ? "PASSWORD_CHANGE_SELF" : "PASSWORD_RESET", email, detail);
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return props.getActorSistema();
        }
        return auth.getName();
    }

    private String clientIp() {
        if (RequestContextHolder.getRequestAttributes() == null) return "n/a";
        try {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
            return request.getRemoteAddr();
        } catch (Exception ex) {
            return "n/a";
        }
    }
}
