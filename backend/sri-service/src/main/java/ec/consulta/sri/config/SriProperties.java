package ec.consulta.sri.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sri")
public record SriProperties(
        String catastroBaseUrl,
        String vehiculoBaseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        Cache cache) {

    public record Cache(Duration contribuyenteTtl, Duration vehiculoTtl, Duration fresh) {}
}
