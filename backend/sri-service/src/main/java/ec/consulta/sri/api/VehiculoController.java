package ec.consulta.sri.api;

import ec.consulta.sri.domain.VehiculoDto;
import ec.consulta.sri.service.VehiculoService;
import ec.consulta.sri.validation.Validaciones;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehiculos")
@Tag(name = "Vehículos", description = "Consulta de vehículos por placa, CAMV o CPN en el SRI")
public class VehiculoController {

    private final VehiculoService service;

    public VehiculoController(VehiculoService service) {
        this.service = service;
    }

    @GetMapping("/{placa}")
    @Operation(summary = "Obtiene la información de un vehículo por placa, CAMV o CPN",
            description = "200 con datos; 400 placa inválida; 404 vehículo no encontrado; "
                    + "503 el SRI no responde y no hay dato guardado.")
    public ResponseEntity<ApiResponse<VehiculoDto>> obtener(@PathVariable String placa) {
        return Respuestas.ok(service.consultar(Validaciones.placa(placa)));
    }
}
