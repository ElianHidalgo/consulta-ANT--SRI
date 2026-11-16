package ec.consulta.ant.service;

import ec.consulta.ant.client.AntClient;
import ec.consulta.ant.client.AntParser;
import ec.consulta.ant.config.AntProperties;
import ec.consulta.ant.domain.LicenciaPuntosDto;
import ec.consulta.ant.domain.Resultado;
import org.springframework.stereotype.Service;

@Service
public class PuntosService {

    private final AntClient client;
    private final AntParser parser;
    private final ResilientFetcher fetcher;
    private final AntProperties props;

    public PuntosService(AntClient client, AntParser parser, ResilientFetcher fetcher, AntProperties props) {
        this.client = client;
        this.parser = parser;
        this.fetcher = fetcher;
        this.props = props;
    }

    /** La placa solo se envía a la ANT; la caché se indexa por cédula porque los puntos son de la persona. */
    public Resultado<LicenciaPuntosDto> consultar(String cedula, String placa) {
        return fetcher.obtener(
                "ant:puntos:" + cedula,
                LicenciaPuntosDto.class,
                props.cache().puntosTtl(),
                props.cache().fresh(),
                "de la ANT",
                () -> parser.parsear(client.obtenerHtml(cedula, placa), cedula));
    }
}
