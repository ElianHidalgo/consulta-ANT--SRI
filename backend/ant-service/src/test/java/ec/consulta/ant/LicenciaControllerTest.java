package ec.consulta.ant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.consulta.ant.api.GlobalExceptionHandler;
import ec.consulta.ant.api.LicenciaController;
import ec.consulta.ant.domain.LicenciaPuntosDto;
import ec.consulta.ant.domain.Resultado;
import ec.consulta.ant.domain.Resultado.Origen;
import ec.consulta.ant.exception.ServicioNoDisponibleException;
import ec.consulta.ant.service.PuntosService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LicenciaControllerTest {

    private static final String CEDULA = "1713175071";

    private PuntosService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        service = mock(PuntosService.class);
        mvc = MockMvcBuilders.standaloneSetup(new LicenciaController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void ok200ConPuntos() throws Exception {
        when(service.consultar(CEDULA, ""))
                .thenReturn(new Resultado<>(new LicenciaPuntosDto(CEDULA, 28, 30), Origen.SERVICIO_EXTERNO, Instant.now(), false, null));

        mvc.perform(get("/api/v1/licencias/" + CEDULA + "/puntos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.puntos").value(28))
                .andExpect(jsonPath("$.meta.desactualizado").value(false));
    }

    @Test
    void cuandoLaAntFallaDevuelveElUltimoDatoYAvisa() throws Exception {
        when(service.consultar(CEDULA, "PCQ1234"))
                .thenReturn(new Resultado<>(new LicenciaPuntosDto(CEDULA, 28, 30), Origen.CACHE, Instant.now(), true, "dato no reciente"));

        mvc.perform(get("/api/v1/licencias/" + CEDULA + "/puntos").param("placa", "pcq-1234"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Data-Source", "CACHE"))
                .andExpect(header().exists("Warning"))
                .andExpect(jsonPath("$.meta.desactualizado").value(true))
                .andExpect(jsonPath("$.meta.advertencia").value("dato no reciente"));
    }

    @Test
    void cedulaInvalidaDevuelve400() throws Exception {
        mvc.perform(get("/api/v1/licencias/123/puntos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.codigo").value("CEDULA_INVALIDA"));
    }

    @Test
    void placaInvalidaDevuelve400() throws Exception {
        mvc.perform(get("/api/v1/licencias/" + CEDULA + "/puntos").param("placa", "@@"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.codigo").value("PLACA_INVALIDA"));
    }

    @Test
    void sinCacheYAntCaidaDevuelve503() throws Exception {
        when(service.consultar(anyString(), anyString())).thenThrow(new ServicioNoDisponibleException("de la ANT"));

        mvc.perform(get("/api/v1/licencias/" + CEDULA + "/puntos"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.codigo").value("SERVICIO_EXTERNO_NO_DISPONIBLE"));
    }
}
