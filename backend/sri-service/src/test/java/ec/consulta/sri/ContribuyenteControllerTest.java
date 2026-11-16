package ec.consulta.sri;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.consulta.sri.api.ContribuyenteController;
import ec.consulta.sri.api.GlobalExceptionHandler;
import ec.consulta.sri.domain.ContribuyenteDto;
import ec.consulta.sri.domain.Resultado;
import ec.consulta.sri.domain.Resultado.Origen;
import ec.consulta.sri.exception.ApiException;
import ec.consulta.sri.exception.ServicioNoDisponibleException;
import ec.consulta.sri.service.ContribuyenteService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Prueba del contrato HTTP (verbos, códigos y errores estructurados) sin levantar Spring ni Redis. */
class ContribuyenteControllerTest {

    private static final String RUC = "1713175071001";

    private ContribuyenteService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        service = mock(ContribuyenteService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ContribuyenteController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static ContribuyenteDto dto() {
        return new ContribuyenteDto(RUC, "PEREZ JUAN", "ACTIVO", "PERSONA NATURAL", "GENERAL", "SERVICIOS", "NO", "2010-01-01", "2024-01-01");
    }

    @Test
    void ok200ConDatosYMeta() throws Exception {
        when(service.consultar(RUC)).thenReturn(new Resultado<>(dto(), Origen.SERVICIO_EXTERNO, Instant.now(), false, null));

        mvc.perform(get("/api/v1/contribuyentes/" + RUC).param("correo", "a@b.com"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Data-Source", "SERVICIO_EXTERNO"))
                .andExpect(jsonPath("$.datos.razonSocial").value("PEREZ JUAN"))
                .andExpect(jsonPath("$.meta.desactualizado").value(false));
    }

    @Test
    void datoDesactualizadoAvisaEnMetaYEnCabecera() throws Exception {
        when(service.consultar(RUC)).thenReturn(new Resultado<>(dto(), Origen.CACHE, Instant.now(), true, "dato no reciente"));

        mvc.perform(get("/api/v1/contribuyentes/" + RUC).param("correo", "a@b.com"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Warning"))
                .andExpect(jsonPath("$.meta.desactualizado").value(true))
                .andExpect(jsonPath("$.meta.advertencia").value("dato no reciente"));
    }

    @Test
    void ruc_invalido_devuelve400ConCodigo() throws Exception {
        mvc.perform(get("/api/v1/contribuyentes/123").param("correo", "a@b.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.codigo").value("RUC_INVALIDO"))
                .andExpect(jsonPath("$.error.mensaje").exists());
    }

    @Test
    void correo_invalido_devuelve400() throws Exception {
        mvc.perform(get("/api/v1/contribuyentes/" + RUC).param("correo", "xxx"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.codigo").value("CORREO_INVALIDO"));
    }

    @Test
    void falta_correo_devuelve400() throws Exception {
        mvc.perform(get("/api/v1/contribuyentes/" + RUC))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.codigo").value("PARAMETRO_REQUERIDO"));
    }

    @Test
    void no_contribuyente_devuelve404() throws Exception {
        when(service.consultar(anyString())).thenThrow(ApiException.notFound("NO_CONTRIBUYENTE", "no existe"));

        mvc.perform(get("/api/v1/contribuyentes/" + RUC).param("correo", "a@b.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.codigo").value("NO_CONTRIBUYENTE"));
    }

    @Test
    void sociedad_devuelve422() throws Exception {
        when(service.consultar(anyString())).thenThrow(ApiException.unprocessable("NO_PERSONA_NATURAL", "es sociedad"));

        mvc.perform(get("/api/v1/contribuyentes/" + RUC).param("correo", "a@b.com"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.codigo").value("NO_PERSONA_NATURAL"));
    }

    @Test
    void servicio_caido_sin_cache_devuelve503ConRetryAfter() throws Exception {
        when(service.consultar(anyString())).thenThrow(new ServicioNoDisponibleException("del SRI"));

        mvc.perform(get("/api/v1/contribuyentes/" + RUC).param("correo", "a@b.com"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.codigo").value("SERVICIO_EXTERNO_NO_DISPONIBLE"));
    }

    @Test
    void metodo_no_permitido_devuelve405() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/contribuyentes/" + RUC))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.codigo").value("METODO_NO_PERMITIDO"));
    }
}
