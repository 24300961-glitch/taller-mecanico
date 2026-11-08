package mx.tallermecanico.app.dashboard.strategy;

import java.util.List;
import mx.tallermecanico.app.auth.UserAccount;
import mx.tallermecanico.app.auth.UserRepository;
import mx.tallermecanico.app.common.Texto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

/** Resuelve la Strategy del dashboard a partir del rol persistido del usuario autenticado. */
@Service
public class DashboardStrategyService {
    private final UserRepository users;
    private final List<DashboardStrategy> strategies;

    /** Construye el resolvedor con las vistas disponibles.
     * @param users repositorio de cuentas
     * @param strategies implementaciones de dashboard descubiertas por Spring
     */
    public DashboardStrategyService(UserRepository users, List<DashboardStrategy> strategies) {
        this.users = users;
        this.strategies = List.copyOf(strategies);
    }

    /** Carga el perfil autenticado y delega el renderizado a la estrategia del rol.
     * @param email correo del principal autenticado
     * @param model modelo Thymeleaf de la solicitud
     * @return vista distinta asociada al rol de la cuenta
     * @throws ResponseStatusException 401 si la cuenta desapareció y 403 si no existe estrategia
     */
    public String render(String email, Model model) {
        UserAccount user = users.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesión ya no corresponde a una cuenta activa"));
        String roleKey = user.getRol() == null ? null : Texto.clave(user.getRol().getNombre());
        return strategies.stream().filter(strategy -> strategy.supports(roleKey)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "No hay un dashboard habilitado para el rol de esta cuenta"))
                .render(user, model);
    }
}
