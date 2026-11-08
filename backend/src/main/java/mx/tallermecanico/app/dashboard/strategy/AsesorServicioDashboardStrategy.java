package mx.tallermecanico.app.dashboard.strategy;

import mx.tallermecanico.app.auth.UserAccount;
import mx.tallermecanico.app.clientes.ClienteRepository;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/** Dashboard de asesoría enfocado en datos de recepción. */
@Component
public final class AsesorServicioDashboardStrategy implements DashboardStrategy {
    private final ClienteRepository clients;

    /** Construye la Strategy de asesoría.
     * @param clients repositorio de clientes
     */
    public AsesorServicioDashboardStrategy(ClienteRepository clients) {
        this.clients = clients;
    }

    /** Identifica el rol asesor.
     * @param roleKey clave normalizada del rol
     * @return true solo para ASESOR_DE_SERVICIO
     */
    @Override public boolean supports(String roleKey) { return "ASESOR_DE_SERVICIO".equals(roleKey); }

    /** Renderiza seguimiento de clientes.
     * @param user asesor autenticado
     * @param model modelo Thymeleaf
     * @return dashboard/asesor-servicio
     */
    @Override public String render(UserAccount user, Model model) {
        model.addAttribute("clientes", clients.count());
        return "dashboard/asesor-servicio";
    }
}
