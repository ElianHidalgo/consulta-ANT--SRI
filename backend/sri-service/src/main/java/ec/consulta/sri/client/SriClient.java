package ec.consulta.sri.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.consulta.sri.config.SriProperties;
import ec.consulta.sri.exception.UpstreamException;
import java.net.URI;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

/** Cliente HTTP de las APIs públicas del SRI. Cualquier fallo se traduce a {@link UpstreamException}. */
@Component
public class SriClient {

    private static final Logger log = LoggerFactory.getLogger(SriClient.class);
    private static final int INTENTOS = 2;

    private final RestClient catastro;
    private final RestClient vehiculo;
    private final ObjectMapper mapper;

    public SriClient(SriProperties props, ObjectMapper mapper) {
        this.mapper = mapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) props.connectTimeout().toMillis());
        factory.setReadTimeout((int) props.readTimeout().toMillis());
        this.catastro = crear(factory, props.catastroBaseUrl());
        this.vehiculo = crear(factory, props.vehiculoBaseUrl());
    }

    private static RestClient crear(SimpleClientHttpRequestFactory factory, String baseUrl) {
        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json, text/plain, */*")
                .defaultHeader(HttpHeaders.USER_AGENT, "consulta-sri-ant/1.0")
                .build();
    }

    /** existePorNumeroRuc: responde true/false en texto plano. */
    public boolean existeRuc(String ruc) {
        String body = obtener(catastro, u -> u.path("/existePorNumeroRuc").queryParam("numeroRuc", ruc).build());
        String valor = body == null ? "" : body.trim();
        if ("true".equalsIgnoreCase(valor)) {
            return true;
        }
        if ("false".equalsIgnoreCase(valor)) {
            return false;
        }
        throw new UpstreamException("Respuesta inesperada del SRI en existePorNumeroRuc");
    }

    /** obtenerPorNumerosRuc: devuelve un arreglo JSON; se toma el primer elemento. */
    public JsonNode obtenerContribuyente(String ruc) {
        String body = obtener(catastro, u -> u.path("/obtenerPorNumerosRuc").queryParam("ruc", ruc).build());
        JsonNode json = parsear(body);
        JsonNode nodo = json.isArray() ? json.path(0) : json;
        if (nodo.isMissingNode() || nodo.isNull() || nodo.isEmpty()) {
            throw new UpstreamException("El SRI no devolvió datos del contribuyente");
        }
        return nodo;
    }

    /** Devuelve null cuando el SRI no tiene el vehículo (cuerpo vacío, null o {}). */
    public JsonNode obtenerVehiculo(String placa) {
        String body = obtener(vehiculo, u -> u
                .path("/obtenerPorNumeroPlacaOPorNumeroCampvOPorNumeroCpn")
                .queryParam("numeroPlacaCampvCpn", placa)
                .build());
        if (body == null || body.isBlank() || "null".equalsIgnoreCase(body.trim())) {
            return null;
        }
        JsonNode json = parsear(body);
        JsonNode nodo = json.isArray() ? json.path(0) : json;
        if (nodo.isMissingNode() || nodo.isNull() || nodo.isEmpty()) {
            return null;
        }
        return nodo;
    }

    private JsonNode parsear(String body) {
        try {
            return mapper.readTree(body == null ? "" : body);
        } catch (Exception e) {
            throw new UpstreamException("El SRI devolvió una respuesta que no es JSON", e);
        }
    }

    private String obtener(RestClient client, Function<UriBuilder, URI> uri) {
        RestClientException ultimo = null;
        for (int intento = 1; intento <= INTENTOS; intento++) {
            try {
                return client.get().uri(uri).retrieve().body(String.class);
            } catch (RestClientException e) {
                ultimo = e;
                log.warn("Fallo al consultar el SRI (intento {}/{}): {}", intento, INTENTOS, e.toString());
            }
        }
        throw new UpstreamException("El SRI no respondió correctamente", ultimo);
    }
}
