package mx.tallermecanico.app.usuarios.strategy;

import mx.tallermecanico.app.auth.UserAccount;
import mx.tallermecanico.app.seguridad.Rol;

/** Estrategia configurada para construir una cuenta de un rol concreto. */
public final class CreacionCuentaPorRolStrategy implements CreacionUsuarioStrategy {
    private final String claveRol;

    /** Configura la estrategia con una clave de rol.
     * @param claveRol clave normalizada del rol
     * @throws IllegalArgumentException si claveRol es null o vacía
     */
    public CreacionCuentaPorRolStrategy(String claveRol) {
        if (claveRol == null || claveRol.isBlank()) throw new IllegalArgumentException("La clave del rol es obligatoria");
        this.claveRol = claveRol;
    }

    /** Comprueba si el rol solicitado corresponde a esta estrategia.
     * @param rol rol persistido solicitado
     * @return true si su clave coincide
     */
    @Override public boolean soporta(Rol rol) { return rol != null && claveRol.equals(rol.getClave()); }

    /** Construye una entidad con el rol protegido contra asignación accidental.
     * @param correo correo ya normalizado
     * @param nombre nombre ya validado
     * @param hashContrasena hash BCrypt
     * @param rol rol de esta estrategia
     * @return cuenta nueva en estado activo
     * @throws IllegalArgumentException si el rol no está soportado
     */
    @Override public UserAccount crear(String correo, String nombre, String hashContrasena, String telefonoRecuperacion, Rol rol) {
        if (!soporta(rol)) throw new IllegalArgumentException("Esta estrategia no soporta el rol solicitado");
        // El rol se recibe del catálogo persistido, nunca del registro público directamente.
        return new UserAccount(correo, nombre, hashContrasena, rol);
    }
}
