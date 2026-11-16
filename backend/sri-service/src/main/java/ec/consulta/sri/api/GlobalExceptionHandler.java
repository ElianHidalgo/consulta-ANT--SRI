package ec.consulta.sri.api;

import ec.consulta.sri.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> api(ApiException e, HttpServletRequest req) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(e.getStatus());
        if (e.getStatus() == HttpStatus.SERVICE_UNAVAILABLE) {
            builder.header(HttpHeaders.RETRY_AFTER, "60");
        }
        return builder.body(cuerpo(e.getCodigo(), e.getMessage(), e.getDetalles(), req));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> parametroFaltante(MissingServletRequestParameterException e, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(cuerpo("PARAMETRO_REQUERIDO",
                "Falta el parámetro obligatorio '" + e.getParameterName() + "'.", List.of(e.getParameterName()), req));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodo(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(cuerpo("METODO_NO_PERMITIDO",
                "El método " + e.getMethod() + " no está permitido en esta ruta.", List.of(), req));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rutaNoEncontrada(NoResourceFoundException e, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpo("RUTA_NO_ENCONTRADA",
                "La ruta solicitada no existe.", List.of(), req));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception e, HttpServletRequest req) {
        log.error("Error no controlado en {}", req.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(cuerpo("ERROR_INTERNO",
                "Ocurrió un error inesperado. Intente nuevamente.", List.of(), req));
    }

    private static ErrorResponse cuerpo(String codigo, String mensaje, List<String> detalles, HttpServletRequest req) {
        return new ErrorResponse(new ErrorResponse.Error(codigo, mensaje, detalles, Instant.now(), req.getRequestURI()));
    }
}
