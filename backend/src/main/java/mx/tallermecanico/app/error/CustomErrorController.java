package mx.tallermecanico.app.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
public class CustomErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(CustomErrorController.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exceptionAttr = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object pathAttr = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (statusAttr != null) {
            try {
                statusCode = Integer.parseInt(statusAttr.toString());
            } catch (NumberFormatException ignored) {
            }
        }

        String errorId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String path = pathAttr != null ? pathAttr.toString() : "desconocido";

        if (statusCode >= 500) {
            if (exceptionAttr instanceof Throwable t) {
                log.error("Error 5xx id={} path={} status={}", errorId, path, statusCode, t);
            } else {
                log.error("Error 5xx id={} path={} status={}", errorId, path, statusCode);
            }
        } else if (statusCode == 403 || statusCode == 401) {
            log.warn("Acceso denegado id={} path={} status={} remote={}",
                    errorId, path, statusCode, request.getRemoteAddr());
        } else if (statusCode == 404) {
            log.warn("Recurso no encontrado id={} path={} status={}", errorId, path, statusCode);
        } else {
            log.info("Error HTTP id={} path={} status={}", errorId, path, statusCode);
        }

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorId", "ERR-" + errorId);
        model.addAttribute("path", path);

        HttpStatus resolved = HttpStatus.resolve(statusCode);
        String mensaje = "Ocurrió un error inesperado.";
        if (resolved != null) {
            mensaje = switch (resolved) {
                case NOT_FOUND -> "La página que buscas no existe o fue movida.";
                case FORBIDDEN -> "No tienes permisos suficientes para acceder a esta sección.";
                case UNAUTHORIZED -> "Tu sesión expiró o no has iniciado sesión.";
                case BAD_REQUEST -> "La solicitud es inválida o contiene datos incorrectos.";
                case METHOD_NOT_ALLOWED -> "El método de solicitud no está permitido para esta ruta.";
                case INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT ->
                    "Error interno del servidor. Inténtalo nuevamente en unos minutos.";
                default -> resolved.getReasonPhrase();
            };
        }
        model.addAttribute("mensaje", mensaje);

        return "error";
    }
}
