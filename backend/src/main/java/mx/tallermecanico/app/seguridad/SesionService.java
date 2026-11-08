package mx.tallermecanico.app.seguridad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class SesionService {

    private static final Logger log = LoggerFactory.getLogger(SesionService.class);

    private final SessionRegistry registry;

    public SesionService(SessionRegistry registry) {
        this.registry = registry;
    }

    public int expirarSesiones(String email) {
        int total = 0;
        for (Object principal : registry.getAllPrincipals()) {
            String nombre = principal instanceof UserDetails ud ? ud.getUsername() : String.valueOf(principal);
            if (!nombre.equalsIgnoreCase(email)) continue;
            for (SessionInformation sesion : registry.getAllSessions(principal, false)) {
                sesion.expireNow();
                total++;
            }
        }
        if (total > 0) {
            log.info("[Sesiones] {} sesión(es) de {} cerradas para aplicar cambios de seguridad", total, email);
        }
        return total;
    }
}
