package com.vetcare.dto;

import com.vetcare.model.enums.EstadoCita;
import java.time.LocalDate;
import java.util.List;

/** Reporte consolidado: totales de las consultas del periodo. */
public record ResumenReporteResponse(
        LocalDate desde,
        LocalDate hasta,
        EstadoCita estado,
        Long veterinarioId,
        long totalConsultas,
        List<ConteoVeterinarioResponse> porVeterinario,
        List<ConteoMotivoResponse> porMotivo) {
}
