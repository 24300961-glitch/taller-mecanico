package mx.tallermecanico.app.auth.strategy;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

/** Strategy de autenticación por perfil; cada implementación define qué perfiles puede validar. */
public interface AutenticacionStrategy {
    /** Describe si esta estrategia corresponde al perfil autenticado.
     * @param usuario perfil cargado desde UserDetailsService
     * @return true cuando esta estrategia acepta el rol del usuario
     * @throws IllegalArgumentException si usuario es null
     */
    boolean soporta(UserDetails usuario);

    /** Verifica las credenciales con el mecanismo asignado a este perfil.
     * @param usuario perfil con sus autoridades
     * @param credencial secreto presentado por el usuario
     * @return Authentication autenticado para Spring Security
     * @throws org.springframework.security.authentication.DisabledException si la cuenta está deshabilitada
     * @throws org.springframework.security.authentication.BadCredentialsException si la credencial no coincide
     */
    Authentication autenticar(UserDetails usuario, String credencial);
}
