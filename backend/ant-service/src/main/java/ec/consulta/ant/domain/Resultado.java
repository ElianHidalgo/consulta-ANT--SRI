package ec.consulta.ant.domain;

import java.time.Instant;

/**
 * Resultado de una consulta: el dato y de dónde salió.
 *
 * @param desactualizado true cuando el servicio externo falló y se devolvió el último dato guardado
 */
public record Resultado<T>(T datos, Origen origen, Instant consultadoEn, boolean desactualizado, String advertencia) {

    public enum Origen {
        /** Dato obtenido en esta misma petición desde el servicio externo. */
        SERVICIO_EXTERNO,
        /** Dato servido desde la caché. */
        CACHE
    }
}
