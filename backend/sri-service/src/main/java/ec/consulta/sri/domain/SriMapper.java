package ec.consulta.sri.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;

/** Convierte el JSON del SRI en los DTO del sistema. Es tolerante a campos ausentes. */
public final class SriMapper {

    private SriMapper() {}

    /** Persona natural según la respuesta del SRI; si no trae el dato, se usa el tercer dígito del RUC. */
    public static boolean esPersonaNatural(JsonNode n, String ruc) {
        String personaSociedad = texto(n, "personaSociedad");
        if (personaSociedad != null) {
            return "PNL".equalsIgnoreCase(personaSociedad);
        }
        String tipo = texto(n, "tipoContribuyente");
        if (tipo != null) {
            return tipo.toUpperCase().contains("NATURAL");
        }
        return ruc.charAt(2) < '6';
    }

    public static ContribuyenteDto contribuyente(JsonNode n, String ruc) {
        JsonNode fechas = n.path("informacionFechasContribuyente");
        return new ContribuyenteDto(
                ruc,
                texto(n, "razonSocial"),
                texto(n, "estadoContribuyenteRuc"),
                texto(n, "tipoContribuyente"),
                texto(n, "regimen"),
                texto(n, "actividadContribuyente"),
                texto(n, "obligadoLlevarContabilidad"),
                texto(fechas, "fechaInicioActividades"),
                texto(fechas, "fechaActualizacion"));
    }

    public static VehiculoDto vehiculo(JsonNode n, String placa) {
        Map<String, String> detalle = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> it = n.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            JsonNode v = e.getValue();
            if (v.isValueNode() && !v.isNull() && !v.asText().isBlank()) {
                detalle.put(e.getKey(), v.asText());
            }
        }
        return new VehiculoDto(
                placa,
                primero(n, "marca", "descripcionMarca", "nombreMarca"),
                primero(n, "modelo", "descripcionModelo", "nombreModelo"),
                primero(n, "anioModelo", "anioFabricacion", "anio", "anioVehiculo"),
                primero(n, "color", "descripcionColor", "colorVehiculo"),
                primero(n, "clase", "descripcionClase", "claseVehiculo"),
                primero(n, "servicio", "descripcionServicio", "tipoServicio"),
                detalle);
    }

    private static String primero(JsonNode n, String... claves) {
        for (String c : claves) {
            String v = texto(n, c);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    static String texto(JsonNode n, String campo) {
        if (n == null) {
            return null;
        }
        JsonNode v = n.get(campo);
        if (v == null || v.isNull() || !v.isValueNode()) {
            return null;
        }
        String s = v.asText().trim();
        return s.isEmpty() ? null : s;
    }
}
