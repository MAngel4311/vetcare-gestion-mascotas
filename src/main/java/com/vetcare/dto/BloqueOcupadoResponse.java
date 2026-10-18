package com.vetcare.dto;

import java.time.LocalDateTime;

/** Una cita PROGRAMADA que ocupa tiempo del veterinario. */
public record BloqueOcupadoResponse(
        Long citaId,
        String mascota,
        LocalDateTime inicio,
        LocalDateTime fin) {
}
