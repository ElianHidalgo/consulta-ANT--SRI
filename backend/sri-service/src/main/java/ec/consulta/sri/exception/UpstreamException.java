package ec.consulta.sri.exception;

/**
 * El servicio externo (SRI) falló: timeout, error de red, HTTP 4xx/5xx o respuesta no interpretable.
 * Es la única excepción que activa el respaldo desde caché.
 */
public class UpstreamException extends RuntimeException {
    public UpstreamException(String mensaje) {
        super(mensaje);
    }

    public UpstreamException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
