package ec.consulta.ant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.consulta.ant.client.AntParser;
import ec.consulta.ant.config.AntProperties;
import ec.consulta.ant.domain.LicenciaPuntosDto;
import ec.consulta.ant.exception.UpstreamException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class AntParserTest {

    private final AntParser parser = new AntParser(new AntProperties(
            "http://localhost", Duration.ofSeconds(1), Duration.ofSeconds(1),
            "(?i)puntos[^0-9]{0,60}(\\d{1,2})",
            new AntProperties.Cache(Duration.ofDays(7), Duration.ofMinutes(5))));

    @Test
    void extraeLosPuntosDelHtml() {
        String html = "<html><body><div><b>Puntos en la licencia:</b> <span>28</span></div></body></html>";

        LicenciaPuntosDto dto = parser.parsear(html, "1713175071");

        assertThat(dto.puntos()).isEqualTo(28);
        assertThat(dto.puntosMaximos()).isEqualTo(30);
        assertThat(dto.cedula()).isEqualTo("1713175071");
    }

    @Test
    void htmlSinPuntosEsRespuestaNoAdecuada() {
        assertThatThrownBy(() -> parser.parsear("<html><body>Servicio en mantenimiento</body></html>", "1713175071"))
                .isInstanceOf(UpstreamException.class);
    }

    @Test
    void puntosFueraDeRangoEsRespuestaNoAdecuada() {
        assertThatThrownBy(() -> parser.parsear("<p>Puntos: 99</p>", "1713175071"))
                .isInstanceOf(UpstreamException.class);
    }

    @Test
    void paginaVaciaEsRespuestaNoAdecuada() {
        assertThatThrownBy(() -> parser.parsear("  ", "1713175071")).isInstanceOf(UpstreamException.class);
    }
}
