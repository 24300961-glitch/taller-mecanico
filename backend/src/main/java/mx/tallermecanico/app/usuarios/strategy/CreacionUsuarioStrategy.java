package mx.tallermecanico.app.usuarios.strategy;

import mx.tallermecanico.app.auth.UserAccount;
import mx.tallermecanico.app.seguridad.Rol;

/** Strategy de creación que establece reglas y perfil inicial para un rol. */
public interface CreacionUsuarioStrategy {
    /** Indica si la estrategia crea cuentas para el rol recibido.
     * @param rol rol persistido solicitado
     * @return true si la clave de rol coincide con esta estrategia
     */
    boolean soporta(Rol rol);

    /** Construye la cuenta aplicando su rol inicial.
     * @param correo correo ya normalizado
     * @param nombre nombre ya validado
     * @param hashContrasena hash BCrypt; nunca texto plano
     * @param telefonoRecuperacion teléfono E.164 sin verificación
     * @param rol rol que esta estrategia soporta
     * @return cuenta lista para persistirse
     * @throws IllegalArgumentException si el rol no corresponde a la estrategia
     */
    UserAccount crear(String correo, String nombre, String hashContrasena, String telefonoRecuperacion, Rol rol);
}
