package ec.consulta.ant.domain;

/** Puntos vigentes de la licencia de una persona según la ANT (máximo 30). */
public record LicenciaPuntosDto(String cedula, int puntos, int puntosMaximos) {

    public static final int PUNTOS_MAXIMOS = 30;
}
