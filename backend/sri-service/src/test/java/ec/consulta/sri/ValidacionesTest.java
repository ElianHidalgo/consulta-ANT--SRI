package ec.consulta.sri;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.consulta.sri.exception.ApiException;
import ec.consulta.sri.validation.Validaciones;
import org.junit.jupiter.api.Test;

class ValidacionesTest {

    @Test
    void aceptaRucDePersonaNaturalConCedulaValida() {
        assertThat(Validaciones.ruc("1713175071001")).isEqualTo("1713175071001");
    }

    @Test
    void rechazaRucConLongitudIncorrecta() {
        assertThatThrownBy(() -> Validaciones.ruc("12345"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("codigo", "RUC_INVALIDO");
    }

    @Test
    void rechazaRucQueNoTerminaEn001() {
        assertThatThrownBy(() -> Validaciones.ruc("1713175071002")).isInstanceOf(ApiException.class);
    }

    @Test
    void rechazaCedulaConDigitoVerificadorIncorrecto() {
        assertThatThrownBy(() -> Validaciones.ruc("1713175072001")).isInstanceOf(ApiException.class);
    }

    @Test
    void rechazaProvinciaInexistente() {
        assertThatThrownBy(() -> Validaciones.ruc("9913175071001")).isInstanceOf(ApiException.class);
    }

    @Test
    void cedulaValida() {
        assertThat(Validaciones.cedulaValida("0900123456")).isTrue();
        assertThat(Validaciones.cedulaValida("0900123457")).isFalse();
        assertThat(Validaciones.cedulaValida("abc")).isFalse();
    }

    @Test
    void normalizaPlaca() {
        assertThat(Validaciones.placa(" pcq-1234 ")).isEqualTo("PCQ1234");
    }

    @Test
    void rechazaPlacaConCaracteresEspeciales() {
        assertThatThrownBy(() -> Validaciones.placa("PC@1234"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("codigo", "PLACA_INVALIDA");
    }

    @Test
    void validaCorreo() {
        assertThat(Validaciones.correo("persona@correo.com")).isEqualTo("persona@correo.com");
        assertThatThrownBy(() -> Validaciones.correo("no-es-correo"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("codigo", "CORREO_INVALIDO");
    }
}
