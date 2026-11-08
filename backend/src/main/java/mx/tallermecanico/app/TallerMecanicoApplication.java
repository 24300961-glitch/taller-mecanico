package mx.tallermecanico.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TallerMecanicoApplication {
    public static void main(String[] args) { SpringApplication.run(TallerMecanicoApplication.class, args); }
}
