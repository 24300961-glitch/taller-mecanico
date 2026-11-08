package mx.tallermecanico.app.auth.strategy;

import java.util.List;
import mx.tallermecanico.app.common.Texto;
import mx.tallermecanico.app.seguridad.SeguridadProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Registra una estrategia independiente por cada rol del taller. */
@Configuration
public class AuthenticationStrategyConfiguration {
    /** Proporciona las políticas de contraseña por perfil.
     * @param encoder PasswordEncoder BCrypt compartido
     * @param properties roles declarados en la configuración del taller
     * @return estrategia de acceso por cada rol configurado
     */
    @Bean("authenticationStrategies") public List<AutenticacionStrategy> authenticationStrategies(
            PasswordEncoder encoder, SeguridadProperties properties) {
        // Cada rol configurado recibe su instancia; un rol nuevo se integra desde app.seguridad.
        return properties.getRolesIniciales().stream().filter(SeguridadProperties.RolInicial::isAsignable)
                .map(role -> (AutenticacionStrategy) new EstrategiaPasswordPorRol(
                        "ROLE_" + Texto.clave(role.getNombre()), encoder)).toList();
    }
}
