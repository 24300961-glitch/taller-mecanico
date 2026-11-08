package mx.tallermecanico.app.config;

import mx.tallermecanico.app.auth.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    @Bean
    ApplicationRunner seedUsers(UserRepository users) {
        return args -> {
            long total = users.count();
            if (total > 0) {
                log.info("[DataLoader] Base de datos inicializada. Total de usuarios registrados: {}", total);
            } else {
                log.info("[DataLoader] Base de datos vacía. Ningún dato precargado.");
                log.info("[DataLoader] Para crear un administrador inicial, define APP_ADMIN_EMAIL y APP_ADMIN_PASSWORD (>=16 caracteres) como variables de entorno antes de iniciar la aplicación.");
            }
        };
    }
}
