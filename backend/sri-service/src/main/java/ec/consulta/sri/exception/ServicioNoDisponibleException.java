package ec.consulta.sri.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

/** El SRI no responde y tampoco existe un dato guardado para esa consulta. */
public class ServicioNoDisponibleException extends ApiException {
    public ServicioNoDisponibleException(String servicio) {
        super(HttpStatus.SERVICE_UNAVAILABLE,
                "SERVICIO_EXTERNO_NO_DISPONIBLE",
                "El servicio " + servicio + " no está disponible y no hay datos guardados de una consulta anterior. "
                        + "Intente nuevamente en unos minutos.",
                List.of());
    }
}
