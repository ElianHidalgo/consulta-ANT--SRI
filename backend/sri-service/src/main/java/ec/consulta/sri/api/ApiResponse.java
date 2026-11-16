package ec.consulta.sri.api;

import java.time.Instant;

/** Contrato de respuesta exitosa: {@code datos} + {@code meta}. */
public record ApiResponse<T>(T datos, Meta meta) {

    /**
     * @param origen          SERVICIO_EXTERNO | CACHE
     * @param desactualizado  true si el servicio externo falló y el dato es el último guardado
     * @param consultadoEn    momento en que el dato se obtuvo realmente del servicio externo
     * @param advertencia     texto para mostrar al usuario cuando el dato no es reciente
     */
    public record Meta(String origen, boolean desactualizado, Instant consultadoEn, String advertencia) {}
}
