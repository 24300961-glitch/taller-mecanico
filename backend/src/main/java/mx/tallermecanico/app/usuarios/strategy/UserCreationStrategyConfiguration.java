package mx.tallermecanico.app.usuarios.strategy;

import java.util.List;
import mx.tallermecanico.app.common.Texto;
import mx.tallermecanico.app.seguridad.SeguridadProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra estrategias de creación para los roles de la configuración del sistema. */
@Configuration
public class UserCreationStrategyConfiguration {
    /** Crea una estrategia por cada rol inicial configurable.
     * @param properties lista de roles declarados en app.seguridad.roles-iniciales
     * @return estrategias indexadas por clave canónica del rol
     */
    @Bean public List<CreacionUsuarioStrategy> userCreationStrategies(SeguridadProperties properties) {
        // La conversión con Texto.clave coincide con la clave que se persiste en Rol.
        return properties.getRolesIniciales().stream().map(r -> (CreacionUsuarioStrategy)
                new CreacionCuentaPorRolStrategy(Texto.clave(r.getNombre()))).toList();
    }
}
