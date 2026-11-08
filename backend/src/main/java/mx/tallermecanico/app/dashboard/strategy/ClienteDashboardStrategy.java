package mx.tallermecanico.app.dashboard.strategy;

import mx.tallermecanico.app.auth.UserAccount;
import mx.tallermecanico.app.vehiculos.VehiculoRepository;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/** Dashboard de cliente que solo muestra sus vehículos y talleres. */
@Component
public final class ClienteDashboardStrategy implements DashboardStrategy {
    private final VehiculoRepository vehicles;

    /** Construye la vista de cliente.
     * @param vehicles repositorio de vehículos
     */
    public ClienteDashboardStrategy(VehiculoRepository vehicles) {
        this.vehicles = vehicles;
    }

    /** Selecciona la vista de cliente.
     * @param roleKey clave canónica del rol
     * @return true para CLIENTE
     */
    @Override public boolean supports(String roleKey) { return "CLIENTE".equals(roleKey); }

    /** Carga vehículos filtrados por la cuenta autenticada.
     * @param user cliente autenticado
     * @param model modelo web
     * @return dashboard/cliente
     */
    @Override public String render(UserAccount user, Model model) {
        // La consulta se filtra en base de datos por correo; no se aceptan IDs enviados por el navegador.
        model.addAttribute("vehiculosCliente", vehicles.findByClienteEmail(user.getEmail()));
        return "dashboard/cliente";
    }
}
