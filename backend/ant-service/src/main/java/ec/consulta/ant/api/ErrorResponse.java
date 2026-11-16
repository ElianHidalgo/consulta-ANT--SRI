package ec.consulta.ant.api;

import java.time.Instant;
import java.util.List;

/** Contrato de error: {@code {"error": {"codigo", "mensaje", "detalles", "timestamp", "ruta"}}}. */
public record ErrorResponse(Error error) {

    public record Error(String codigo, String mensaje, List<String> detalles, Instant timestamp, String ruta) {}
}
