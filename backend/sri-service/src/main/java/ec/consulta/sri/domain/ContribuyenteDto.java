package ec.consulta.sri.domain;

public record ContribuyenteDto(
        String ruc,
        String razonSocial,
        String estado,
        String tipoContribuyente,
        String regimen,
        String actividadEconomica,
        String obligadoContabilidad,
        String fechaInicioActividades,
        String fechaActualizacion) {}
