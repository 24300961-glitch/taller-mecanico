package mx.tallermecanico.app.error;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    public static ResponseEntity<Map<String, Object>> cuerpo(HttpStatusCode status, String mensaje, Map<String, Object> extra) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("message", mensaje);
        if (extra != null) body.putAll(extra);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Map<String, Object>> negocio(NegocioException e) {
        return cuerpo(e.getStatus(), e.getMessage(), e.getDatos());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> known(ResponseStatusException e) {
        return cuerpo(e.getStatusCode(), e.getReason() == null ? "Solicitud no válida" : e.getReason(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException e) {
        Map<String, String> errores = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> errores.putIfAbsent(f.getField(), f.getDefaultMessage()));
        e.getBindingResult().getGlobalErrors().forEach(g -> errores.putIfAbsent(g.getObjectName(), g.getDefaultMessage()));
        return cuerpo(HttpStatus.BAD_REQUEST, "Revisa los campos marcados: hay datos inválidos o incompletos.", Map.of("errores", errores));
    }

    /** Devuelve una validación de campo cuando el archivo rebasa el límite multipart.
     * @param e excepción de tamaño máximo
     * @return 400 con el campo foto y su causa
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> uploadTooLarge(MaxUploadSizeExceededException e) {
        return cuerpo(HttpStatus.BAD_REQUEST, "La foto no debe exceder 15 MB.",
                Map.of("errores", Map.of("foto", "La foto no debe exceder 15 MB.")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable() {
        return cuerpo(HttpStatus.BAD_REQUEST, "El formato de los datos enviados no es válido.", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> mismatch(MethodArgumentTypeMismatchException e) {
        return cuerpo(HttpStatus.BAD_REQUEST, "El valor del parámetro \"" + e.getName() + "\" no es válido.", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> method() {
        return cuerpo(HttpStatus.METHOD_NOT_ALLOWED, "Operación no permitida en esta ruta.", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> denied(AccessDeniedException e, HttpServletRequest req) {
        log.warn("[Security] Permiso insuficiente en {} para {}", req.getRequestURI(), req.getRemoteUser());
        return cuerpo(HttpStatus.FORBIDDEN, "Tu rol no tiene permiso para realizar esta acción.", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integrity(DataIntegrityViolationException e, HttpServletRequest req) {
        log.warn("[Datos] Violación de integridad en {}: {}", req.getRequestURI(), e.getMostSpecificCause().getMessage());
        return cuerpo(HttpStatus.CONFLICT, "El registro entra en conflicto con datos existentes (posible duplicado).", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception e, HttpServletRequest req) {
        String id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.error("Fallo no controlado id={} path={}", id, req.getRequestURI(), e);
        return cuerpo(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Intenta más tarde. Si persiste, reporta el folio ERR-" + id + ".",
                Map.of("errorId", "ERR-" + id));
    }
}
