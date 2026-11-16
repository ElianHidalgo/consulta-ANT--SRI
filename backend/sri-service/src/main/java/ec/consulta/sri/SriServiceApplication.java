package ec.consulta.sri;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SriServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SriServiceApplication.class, args);
    }
}
