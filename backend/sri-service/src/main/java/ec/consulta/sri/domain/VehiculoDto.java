package ec.consulta.sri.domain;

import java.util.Map;

/**
 * Datos del vehículo. Los campos principales se mapean de varios nombres posibles del SRI y
 * {@code detalle} conserva todos los valores escalares de la respuesta original.
 */
public record VehiculoDto(
        String placa,
        String marca,
        String modelo,
        String anio,
        String color,
        String clase,
        String servicio,
        Map<String, String> detalle) {}
