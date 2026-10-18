package com.vetcare.dto;

import com.vetcare.model.enums.EstadoCita;
import com.vetcare.model.enums.MotivoCita;
import java.time.LocalDateTime;

public record CitaResponse(
        Long id,
        Long mascotaId,
        String mascotaNombre,
        String duenoNombre,
        Long veterinarioId,
        String veterinarioNombre,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        MotivoCita motivo,
        EstadoCita estado,
        String motivoCancelacion) {
}
