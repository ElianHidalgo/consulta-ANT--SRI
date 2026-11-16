package ec.consulta.ant.client;

import ec.consulta.ant.config.AntProperties;
import ec.consulta.ant.exception.UpstreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Cliente HTTP de la web de citaciones de la ANT (devuelve HTML). */
@Component
public class AntClient {

    private static final Logger log = LoggerFactory.getLogger(AntClient.class);
    private static final int INTENTOS = 2;

    private final RestClient client;

    public AntClient(AntProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) props.connectTimeout().toMillis());
        factory.setReadTimeout((int) props.readTimeout().toMillis());
        this.client = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(props.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, "consulta-sri-ant/1.0")
                .defaultHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml")
                .build();
    }

    public String obtenerHtml(String cedula, String placa) {
        RestClientException ultimo = null;
        for (int intento = 1; intento <= INTENTOS; intento++) {
            try {
                return client.get()
                        .uri(u -> u.queryParam("ps_tipo_identificacion", "CED")
                                .queryParam("ps_identificacion", cedula)
                                .queryParam("ps_placa", placa == null ? "" : placa)
                                .build())
                        .retrieve()
                        .body(String.class);
            } catch (RestClientException e) {
                ultimo = e;
                log.warn("Fallo al consultar la ANT (intento {}/{}): {}", intento, INTENTOS, e.toString());
            }
        }
        throw new UpstreamException("La ANT no respondió correctamente", ultimo);
    }
}
