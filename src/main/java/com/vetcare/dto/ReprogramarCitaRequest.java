package com.vetcare.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** F-06 / SWR-11. */
public record ReprogramarCitaRequest(
        @NotNull(message = "La nueva fecha y hora de inicio son obligatorias")
        LocalDateTime fechaHoraInicio,

        @NotNull(message = "La nueva fecha y hora de fin son obligatorias")
        LocalDateTime fechaHoraFin) {
}
