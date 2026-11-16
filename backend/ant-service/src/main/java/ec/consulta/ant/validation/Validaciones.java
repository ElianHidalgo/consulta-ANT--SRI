package ec.consulta.ant.validation;

import ec.consulta.ant.exception.ApiException;
import java.util.regex.Pattern;

public final class Validaciones {

    private static final Pattern PLACA = Pattern.compile("^[A-Z0-9]{5,17}$");

    private Validaciones() {}

    public static String cedula(String valor) {
        String v = valor == null ? "" : valor.trim();
        if (!v.matches("\\d{10}")) {
            throw ApiException.badRequest("CEDULA_INVALIDA", "La cédula debe tener exactamente 10 dígitos numéricos.");
        }
        if (!cedulaValida(v)) {
            throw ApiException.badRequest("CEDULA_INVALIDA", "La cédula no es válida (dígito verificador incorrecto).");
        }
        return v;
    }

    /** La placa es opcional: null o vacío se devuelve como cadena vacía. */
    public static String placaOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return "";
        }
        String v = valor.trim().toUpperCase().replace("-", "").replace(" ", "");
        if (!PLACA.matcher(v).matches()) {
            throw ApiException.badRequest("PLACA_INVALIDA",
                    "La placa debe tener entre 5 y 17 caracteres alfanuméricos (ej. PCQ1234).");
        }
        return v;
    }

    /** Algoritmo módulo 10 de la cédula ecuatoriana. */
    public static boolean cedulaValida(String c) {
        if (c == null || !c.matches("\\d{10}")) {
            return false;
        }
        int provincia = Integer.parseInt(c.substring(0, 2));
        if (!((provincia >= 1 && provincia <= 24) || provincia == 30) || c.charAt(2) >= '6') {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < 9; i++) {
            int d = (c.charAt(i) - '0') * (i % 2 == 0 ? 2 : 1);
            suma += d > 9 ? d - 9 : d;
        }
        return (10 - suma % 10) % 10 == c.charAt(9) - '0';
    }
}
