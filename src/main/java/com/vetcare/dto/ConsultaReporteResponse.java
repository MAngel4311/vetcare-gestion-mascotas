package com.vetcare.dto;

import com.vetcare.model.enums.EstadoCita;
import com.vetcare.model.enums.MotivoCita;
import java.time.LocalDateTime;

/** Una fila del reporte de consultas (F-07 / SWR-12). */
public record ConsultaReporteResponse(
        Long citaId,
        LocalDateTime fecha,
        String mascota,
        String veterinario,
        MotivoCita motivo,
        EstadoCita estado) {
}
