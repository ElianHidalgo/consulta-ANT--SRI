package ec.consulta.ant.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

/** Error de negocio o de validación con código estable para el cliente. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;
    private final List<String> detalles;

    public ApiException(HttpStatus status, String codigo, String mensaje, List<String> detalles) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
        this.detalles = detalles == null ? List.of() : detalles;
    }

    public static ApiException badRequest(String codigo, String mensaje, String... detalles) {
        return new ApiException(HttpStatus.BAD_REQUEST, codigo, mensaje, List.of(detalles));
    }

    public static ApiException notFound(String codigo, String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, codigo, mensaje, List.of());
    }

    public static ApiException unprocessable(String codigo, String mensaje) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, codigo, mensaje, List.of());
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    public List<String> getDetalles() {
        return detalles;
    }
}
