package mx.tallermecanico.app.dashboard.strategy;

import mx.tallermecanico.app.auth.UserAccount;
import org.springframework.ui.Model;

/** Strategy que prepara el modelo y la vista inicial de un rol. */
public interface DashboardStrategy {
    /** Define si esta Strategy aplica al rol indicado.
     * @param roleKey clave normalizada del rol
     * @return true cuando esta vista corresponde al rol
     */
    boolean supports(String roleKey);

    /** Agrega datos de panel y devuelve el nombre de vista Thymeleaf.
     * @param user cuenta autenticada
     * @param model modelo de la solicitud web
     * @return nombre de template relativo a resources/templates
     */
    String render(UserAccount user, Model model);
}
