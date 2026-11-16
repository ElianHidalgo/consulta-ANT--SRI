package ec.consulta.ant.client;

import ec.consulta.ant.config.AntProperties;
import ec.consulta.ant.domain.LicenciaPuntosDto;
import ec.consulta.ant.exception.UpstreamException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/**
 * Extrae los puntos de la licencia del HTML de la ANT.
 *
 * <p>La página no ofrece una API: el texto visible se busca con la expresión {@code ant.puntos-regex}
 * (grupo 1 = puntos). Si no se encuentra o el valor no está entre 0 y 30, la respuesta se considera
 * "no adecuada" ({@link UpstreamException}) y NO se guarda en caché, de modo que la caché solo
 * contiene datos buenos.
 */
@Component
public class AntParser {

    private final Pattern patron;

    public AntParser(AntProperties props) {
        this.patron = Pattern.compile(props.puntosRegex());
    }

    public LicenciaPuntosDto parsear(String html, String cedula) {
        if (html == null || html.isBlank()) {
            throw new UpstreamException("La ANT devolvió una página vacía");
        }
        String texto = Jsoup.parse(html).text();
        Matcher m = patron.matcher(texto);
        if (!m.find()) {
            throw new UpstreamException("La respuesta de la ANT no contiene los puntos de la licencia (estructura inesperada)");
        }
        int puntos;
        try {
            puntos = Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            throw new UpstreamException("Valor de puntos no numérico en la respuesta de la ANT", e);
        }
        if (puntos < 0 || puntos > LicenciaPuntosDto.PUNTOS_MAXIMOS) {
            throw new UpstreamException("Valor de puntos fuera de rango en la respuesta de la ANT: " + puntos);
        }
        return new LicenciaPuntosDto(cedula, puntos, LicenciaPuntosDto.PUNTOS_MAXIMOS);
    }
}
