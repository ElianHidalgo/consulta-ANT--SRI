package ec.consulta.ant.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ant")
public record AntProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        String puntosRegex,
        Cache cache) {

    public record Cache(Duration puntosTtl, Duration fresh) {}
}
