package mx.tallermecanico.app.dashboard.strategy;

import mx.tallermecanico.app.auth.UserAccount;
import mx.tallermecanico.app.auth.UserRepository;
import mx.tallermecanico.app.clientes.ClienteRepository;
import mx.tallermecanico.app.vehiculos.VehiculoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/** Dashboard global con indicadores administrativos del taller. */
@Component
public final class AdministradorDashboardStrategy implements DashboardStrategy {
    private final UserRepository users;
    private final ClienteRepository clients;
    private final VehiculoRepository vehicles;
    @Value("${spring.datasource.url}") private String datasourceUrl;

    /** Construye el panel con los repositorios de indicadores.
     * @param users repositorio de usuarios
     * @param clients repositorio de clientes
     * @param vehicles repositorio de vehículos
     */
    public AdministradorDashboardStrategy(UserRepository users, ClienteRepository clients,
            VehiculoRepository vehicles) {
        this.users = users; this.clients = clients; this.vehicles = vehicles;
    }

    /** Selecciona la vista para el rol administrador.
     * @param roleKey clave canónica del rol
     * @return true para ADMINISTRADOR_DE_TALLER
     */
    @Override public boolean supports(String roleKey) { return "ADMINISTRADOR_DE_TALLER".equals(roleKey); }

    /** Agrega estadísticas globales y renderiza el panel de administración.
     * @param user administrador autenticado
     * @param model modelo web
     * @return dashboard/administrador
     */
    @Override public String render(UserAccount user, Model model) {
        model.addAttribute("totalUsuarios", users.count());
        model.addAttribute("totalClientes", clients.count());
        model.addAttribute("totalVehiculos", vehicles.count());
        model.addAttribute("dashboardTitulo", "Resumen administrativo");
        model.addAttribute("motorDb", databaseName());
        return "dashboard/administrador";
    }

    /** Traduce la URL de conexión a una etiqueta sin exponer credenciales.
     * @return nombre del motor de base de datos
     */
    private String databaseName() {
        if (datasourceUrl.contains("mysql")) return "MySQL";
        if (datasourceUrl.contains("h2")) return "H2";
        if (datasourceUrl.contains("postgresql")) return "PostgreSQL";
        return "Base de datos";
    }
}
