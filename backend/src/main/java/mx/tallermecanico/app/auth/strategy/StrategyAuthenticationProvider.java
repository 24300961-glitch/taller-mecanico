package mx.tallermecanico.app.auth.strategy;

import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;

/** Adapta las estrategias de perfil al proveedor de autenticación de Spring Security. */
@Component
public final class StrategyAuthenticationProvider implements AuthenticationProvider {
    private final UserDetailsService users;
    private final List<AutenticacionStrategy> strategies;

    /** Construye el proveedor con las estrategias disponibles.
     * @param users servicio de carga de cuentas
     * @param configuredStrategies estrategias base por rol
     * @param extensionStrategies beans de estrategias adicionales o especializadas
     * @throws IllegalArgumentException si la lista está vacía
     */
    public StrategyAuthenticationProvider(UserDetailsService users,
            @Qualifier("authenticationStrategies") List<AutenticacionStrategy> configuredStrategies,
            ObjectProvider<AutenticacionStrategy> extensionStrategies) {
        this.users = users;
        // Una Strategy añadida como bean puede reemplazar políticas base sin editar este proveedor.
        java.util.ArrayList<AutenticacionStrategy> ordered = new java.util.ArrayList<>(extensionStrategies.orderedStream().toList());
        ordered.addAll(configuredStrategies);
        this.strategies = List.copyOf(ordered);
        if (this.strategies.isEmpty()) throw new IllegalArgumentException("Debe configurarse al menos una estrategia de autenticación");
    }

    /** Carga la cuenta y delega en la primera estrategia compatible.
     * @param request credenciales recibidas por Spring Security
     * @return resultado autenticado
     * @throws BadCredentialsException si ninguna estrategia soporta el rol
     */
    @Override public Authentication authenticate(Authentication request) {
        if (!(request.getPrincipal() instanceof String email) || !(request.getCredentials() instanceof String password)) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        UserDetails user = users.loadUserByUsername(email);
        return strategies.stream().filter(strategy -> strategy.soporta(user)).findFirst()
                .orElseThrow(() -> new BadCredentialsException("No hay una estrategia de acceso configurada para este rol"))
                .autenticar(user, password);
    }

    /** Declara los tokens de formulario que puede procesar este proveedor.
     * @param authentication clase del token recibido
     * @return true para el flujo estándar email/contraseña
     */
    @Override public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
