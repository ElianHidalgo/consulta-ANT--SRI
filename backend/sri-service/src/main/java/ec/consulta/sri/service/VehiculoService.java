package ec.consulta.sri.service;

import com.fasterxml.jackson.databind.JsonNode;
import ec.consulta.sri.client.SriClient;
import ec.consulta.sri.config.SriProperties;
import ec.consulta.sri.domain.Resultado;
import ec.consulta.sri.domain.SriMapper;
import ec.consulta.sri.domain.VehiculoDto;
import ec.consulta.sri.exception.ApiException;
import org.springframework.stereotype.Service;

@Service
public class VehiculoService {

    private final SriClient client;
    private final ResilientFetcher fetcher;
    private final SriProperties props;

    public VehiculoService(SriClient client, ResilientFetcher fetcher, SriProperties props) {
        this.client = client;
        this.fetcher = fetcher;
        this.props = props;
    }

    public Resultado<VehiculoDto> consultar(String placa) {
        return fetcher.obtener(
                "sri:vehiculo:" + placa,
                VehiculoDto.class,
                props.cache().vehiculoTtl(),
                props.cache().fresh(),
                "del SRI",
                () -> {
                    JsonNode datos = client.obtenerVehiculo(placa);
                    if (datos == null) {
                        throw ApiException.notFound("VEHICULO_NO_ENCONTRADO",
                                "No se encontró un vehículo con la placa/CAMV/CPN " + placa + ".");
                    }
                    return SriMapper.vehiculo(datos, placa);
                });
    }
}
