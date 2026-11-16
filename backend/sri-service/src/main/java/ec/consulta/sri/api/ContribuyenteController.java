package ec.consulta.sri.api;

import ec.consulta.sri.domain.ContribuyenteDto;
import ec.consulta.sri.service.ContribuyenteService;
import ec.consulta.sri.validation.Validaciones;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contribuyentes")
@Tag(name = "Contribuyentes", description = "Consulta de contribuyentes persona natural en el SRI")
public class ContribuyenteController {

    private static final Logger log = LoggerFactory.getLogger(ContribuyenteController.class);

    private final ContribuyenteService service;

    public ContribuyenteController(ContribuyenteService service) {
        this.service = service;
    }

    @GetMapping("/{ruc}")
    @Operation(summary = "Verifica que el RUC sea contribuyente persona natural y devuelve sus datos",
            description = "200 con datos (meta.desactualizado=true si el SRI falló y se usó el último dato guardado); "
                    + "400 entrada inválida; 404 no es contribuyente; 422 no es persona natural; "
                    + "503 el SRI no responde y no hay dato guardado.")
    public ResponseEntity<ApiResponse<ContribuyenteDto>> obtener(
            @PathVariable String ruc, @RequestParam String correo) {
        String correoValido = Validaciones.correo(correo);
        String rucValido = Validaciones.ruc(ruc);
        log.info("Consulta de contribuyente ruc={} correo={}", rucValido, enmascarar(correoValido));
        return Respuestas.ok(service.consultar(rucValido));
    }

    private static String enmascarar(String correo) {
        int arroba = correo.indexOf('@');
        return arroba <= 1 ? "***" : correo.charAt(0) + "***" + correo.substring(arroba);
    }
}
