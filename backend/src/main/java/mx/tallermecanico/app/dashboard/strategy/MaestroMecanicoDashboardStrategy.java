package mx.tallermecanico.app.dashboard.strategy;

import mx.tallermecanico.app.auth.UserAccount;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/** Dashboard del área mecánica (el módulo de agenda fue retirado). */
@Component
public final class MaestroMecanicoDashboardStrategy implements DashboardStrategy {

    /** Selecciona la vista del maestro mecánico.
     * @param roleKey clave canónica del rol
     * @return true para MAESTRO_MECANICO
     */
    @Override public boolean supports(String roleKey) { return "MAESTRO_MECANICO".equals(roleKey); }

    /** Renderiza el panel del área mecánica.
     * @param user maestro mecánico autenticado
     * @param model modelo web
     * @return dashboard/maestro-mecanico
     */
    @Override public String render(UserAccount user, Model model) {
        return "dashboard/maestro-mecanico";
    }
}
