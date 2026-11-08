package mx.tallermecanico.app.auth.strategy;

import java.util.Objects;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Implementación de Strategy que valida contraseña BCrypt para un rol específico. */
public final class EstrategiaPasswordPorRol implements AutenticacionStrategy {
    private final String autoridad;
    private final PasswordEncoder encoder;

    /** Crea la estrategia para una autoridad de rol.
     * @param autoridad autoridad completa, por ejemplo ROLE_CLIENTE
     * @param encoder codificador configurado por Spring Security
     * @throws IllegalArgumentException si autoridad o encoder son null/vacíos
     */
    public EstrategiaPasswordPorRol(String autoridad, PasswordEncoder encoder) {
        if (autoridad == null || autoridad.isBlank() || encoder == null) throw new IllegalArgumentException("Rol y encoder son obligatorios");
        this.autoridad = autoridad;
        this.encoder = encoder;
    }

    /** Comprueba si el usuario tiene el rol configurado.
     * @param usuario perfil cargado desde UserDetailsService
     * @return true si incluye la autoridad de esta estrategia
     * @throws IllegalArgumentException si usuario es null
     */
    @Override public boolean soporta(UserDetails usuario) {
        if (usuario == null) throw new IllegalArgumentException("El usuario es obligatorio");
        return usuario.getAuthorities().contains(new SimpleGrantedAuthority(autoridad));
    }

    /** Verifica BCrypt y emite el Authentication autenticado.
     * @param usuario perfil cargado desde UserDetailsService
     * @param credencial contraseña enviada por el formulario
     * @return token autenticado sin incluir la contraseña en el principal
     * @throws DisabledException si la cuenta está deshabilitada
     * @throws BadCredentialsException si el rol o la contraseña no coinciden
     */
    @Override public Authentication autenticar(UserDetails usuario, String credencial) {
        if (!usuario.isEnabled()) throw new DisabledException("Cuenta deshabilitada");
        if (!soporta(usuario) || credencial == null || !encoder.matches(credencial, usuario.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        // El token no conserva el secreto recibido, solo el principal y sus permisos.
        return UsernamePasswordAuthenticationToken.authenticated(usuario, null, usuario.getAuthorities());
    }
}
