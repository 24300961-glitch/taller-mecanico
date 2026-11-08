package mx.tallermecanico.app.dashboard.strategy;

import mx.tallermecanico.app.auth.UserAccount;
import mx.tallermecanico.app.clientes.ClienteRepository;
import mx.tallermecanico.app.vehiculos.VehiculoRepository;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/** Dashboard operativo de recepción para clientes y vehículos. */
@Component
public final class RecepcionistaDashboardStrategy implements DashboardStrategy {
    private final ClienteRepository clients;
    private final VehiculoRepository vehicles;

    /** Construye la estrategia de recepción.
     * @param clients repositorio de clientes
     * @param vehicles repositorio de vehículos
     */
    public RecepcionistaDashboardStrategy(ClienteRepository clients, VehiculoRepository vehicles) {
        this.clients = clients; this.vehicles = vehicles;
    }

    /** Selecciona la vista de recepción.
     * @param roleKey clave canónica del rol
     * @return true para RECEPCIONISTA
     */
    @Override public boolean supports(String roleKey) { return "RECEPCIONISTA".equals(roleKey); }

    /** Presenta la carga de atención diaria.
     * @param user recepcionista autenticado
     * @param model modelo web
     * @return dashboard/recepcionista
     */
    @Override public String render(UserAccount user, Model model) {
        model.addAttribute("clientes", clients.count());
        model.addAttribute("vehiculos", vehicles.count());
        return "dashboard/recepcionista";
    }
}
