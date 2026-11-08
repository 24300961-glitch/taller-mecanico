package mx.tallermecanico.app.dashboard.strategy;

import mx.tallermecanico.app.auth.UserAccount;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/** Dashboard de consulta técnica (el módulo de agenda fue retirado). */
@Component
public final class TecnicoConsultaDashboardStrategy implements DashboardStrategy {

    /** Identifica el rol técnico de consulta.
     * @param roleKey clave normalizada del rol
     * @return true solo para TECNICO_DE_CONSULTA
     */
    @Override public boolean supports(String roleKey) { return "TECNICO_DE_CONSULTA".equals(roleKey); }

    /** Renderiza el panel de consulta técnica.
     * @param user técnico autenticado
     * @param model modelo Thymeleaf
     * @return dashboard/tecnico-consulta
     */
    @Override public String render(UserAccount user, Model model) {
        return "dashboard/tecnico-consulta";
    }
}
