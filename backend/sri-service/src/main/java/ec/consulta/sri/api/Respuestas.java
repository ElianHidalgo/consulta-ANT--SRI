package ec.consulta.sri.api;

import ec.consulta.sri.domain.Resultado;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

final class Respuestas {

    private Respuestas() {}

    static <T> ResponseEntity<ApiResponse<T>> ok(Resultado<T> r) {
        ApiResponse.Meta meta = new ApiResponse.Meta(r.origen().name(), r.desactualizado(), r.consultadoEn(), r.advertencia());
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok().header("X-Data-Source", r.origen().name());
        if (r.desactualizado()) {
            // RFC 7234: 110 = Response is Stale
            builder.header(HttpHeaders.WARNING, "110 - \"Response is Stale\"");
        }
        return builder.body(new ApiResponse<>(r.datos(), meta));
    }
}
