package ec.consulta.sri.service;

import com.fasterxml.jackson.databind.JsonNode;
import ec.consulta.sri.client.SriClient;
import ec.consulta.sri.config.SriProperties;
import ec.consulta.sri.domain.ContribuyenteDto;
import ec.consulta.sri.domain.Resultado;
import ec.consulta.sri.domain.SriMapper;
import ec.consulta.sri.exception.ApiException;
import org.springframework.stereotype.Service;

@Service
public class ContribuyenteService {

    private final SriClient client;
    private final ResilientFetcher fetcher;
    private final SriProperties props;

    public ContribuyenteService(SriClient client, ResilientFetcher fetcher, SriProperties props) {
        this.client = client;
        this.fetcher = fetcher;
        this.props = props;
    }

    /** Verifica que el RUC sea contribuyente, que sea persona natural y devuelve sus datos. */
    public Resultado<ContribuyenteDto> consultar(String ruc) {
        return fetcher.obtener(
                "sri:contribuyente:" + ruc,
                ContribuyenteDto.class,
                props.cache().contribuyenteTtl(),
                props.cache().fresh(),
                "del SRI",
                () -> {
                    if (!client.existeRuc(ruc)) {
                        throw ApiException.notFound("NO_CONTRIBUYENTE",
                                "El RUC " + ruc + " no está registrado como contribuyente en el SRI.");
                    }
                    JsonNode datos = client.obtenerContribuyente(ruc);
                    if (!SriMapper.esPersonaNatural(datos, ruc)) {
                        throw ApiException.unprocessable("NO_PERSONA_NATURAL",
                                "El RUC " + ruc + " corresponde a una sociedad; este sistema solo atiende personas naturales.");
                    }
                    return SriMapper.contribuyente(datos, ruc);
                });
    }
}
