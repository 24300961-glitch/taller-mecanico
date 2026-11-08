package mx.tallermecanico.app.error;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class NegocioException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, Object> datos;

    public NegocioException(HttpStatus status, String mensaje, Map<String, Object> datos) {
        super(mensaje);
        this.status = status;
        this.datos = datos;
    }

    public NegocioException(HttpStatus status, String mensaje) {
        this(status, mensaje, null);
    }

    public HttpStatus getStatus() { return status; }
    public Map<String, Object> getDatos() { return datos; }
}
