package ec.consulta.ant.api;

import ec.consulta.ant.domain.LicenciaPuntosDto;
import ec.consulta.ant.service.PuntosService;
import ec.consulta.ant.validation.Validaciones;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/licencias")
@Tag(name = "Licencias", description = "Puntos de la licencia de conducir según la ANT")
public class LicenciaController {

    private final PuntosService service;

    public LicenciaController(PuntosService service) {
        this.service = service;
    }

    @GetMapping("/{cedula}/puntos")
    @Operation(summary = "Obtiene los puntos vigentes de la licencia de una persona",
            description = "200 con datos (meta.desactualizado=true si la ANT falló y se usó el último dato guardado); "
                    + "400 cédula o placa inválidas; 503 la ANT no responde y no hay dato guardado.")
    public ResponseEntity<ApiResponse<LicenciaPuntosDto>> puntos(
            @PathVariable String cedula, @RequestParam(required = false) String placa) {
        return Respuestas.ok(service.consultar(Validaciones.cedula(cedula), Validaciones.placaOpcional(placa)));
    }
}
