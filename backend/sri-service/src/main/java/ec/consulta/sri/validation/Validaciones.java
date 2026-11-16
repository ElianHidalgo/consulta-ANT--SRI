package ec.consulta.sri.validation;

import ec.consulta.sri.exception.ApiException;
import java.util.regex.Pattern;

/** Validación de entradas. Lanza {@link ApiException} 400 con un código estable. */
public final class Validaciones {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PLACA = Pattern.compile("^[A-Z0-9]{5,17}$");

    private Validaciones() {}

    public static String correo(String valor) {
        String v = valor == null ? "" : valor.trim();
        if (v.length() > 254 || !EMAIL.matcher(v).matches()) {
            throw ApiException.badRequest("CORREO_INVALIDO", "El correo electrónico no tiene un formato válido.");
        }
        return v;
    }

    public static String ruc(String valor) {
        String v = valor == null ? "" : valor.trim();
        if (!v.matches("\\d{13}")) {
            throw ApiException.badRequest("RUC_INVALIDO", "El RUC debe tener exactamente 13 dígitos numéricos.");
        }
        if (!provinciaValida(v)) {
            throw ApiException.badRequest("RUC_INVALIDO", "Los dos primeros dígitos del RUC no corresponden a una provincia válida.");
        }
        if (!v.endsWith("001")) {
            throw ApiException.badRequest("RUC_INVALIDO", "El RUC de una persona natural termina en 001.");
        }
        boolean personaNatural = v.charAt(2) < '6';
        if (personaNatural && !cedulaValida(v.substring(0, 10))) {
            throw ApiException.badRequest("RUC_INVALIDO", "Los primeros 10 dígitos del RUC no son una cédula válida.");
        }
        return v;
    }

    public static String placa(String valor) {
        String v = valor == null ? "" : valor.trim().toUpperCase().replace("-", "").replace(" ", "");
        if (!PLACA.matcher(v).matches()) {
            throw ApiException.badRequest("PLACA_INVALIDA",
                    "La placa, CAMV o CPN debe tener entre 5 y 17 caracteres alfanuméricos (ej. PCQ1234).");
        }
        return v;
    }

    private static boolean provinciaValida(String digitos) {
        int provincia = Integer.parseInt(digitos.substring(0, 2));
        return (provincia >= 1 && provincia <= 24) || provincia == 30;
    }

    /** Algoritmo módulo 10 de la cédula ecuatoriana. */
    public static boolean cedulaValida(String c) {
        if (c == null || !c.matches("\\d{10}") || !provinciaValida(c) || c.charAt(2) >= '6') {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < 9; i++) {
            int d = (c.charAt(i) - '0') * (i % 2 == 0 ? 2 : 1);
            suma += d > 9 ? d - 9 : d;
        }
        int verificador = (10 - suma % 10) % 10;
        return verificador == c.charAt(9) - '0';
    }
}
