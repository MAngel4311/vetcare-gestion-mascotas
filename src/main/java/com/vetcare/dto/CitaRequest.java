package com.vetcare.dto;

import com.vetcare.model.enums.MotivoCita;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** F-02 / SWR-02, SWR-03. Fechas en formato ISO, ej. 2026-11-10T09:00:00. */
public record CitaRequest(
        @NotNull(message = "La mascota es obligatoria")
        Long mascotaId,

        @NotNull(message = "El veterinario es obligatorio")
        Long veterinarioId,

        @NotNull(message = "La fecha y hora de inicio son obligatorias")
        LocalDateTime fechaHoraInicio,

        @NotNull(message = "La fecha y hora de fin son obligatorias")
        LocalDateTime fechaHoraFin,

        @NotNull(message = "El motivo de la cita es obligatorio")
        MotivoCita motivo) {
}
