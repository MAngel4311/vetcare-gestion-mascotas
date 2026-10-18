package com.vetcare.dto;

import java.time.LocalDate;

public record VacunacionResponse(
        Long id,
        Long mascotaId,
        String mascotaNombre,
        Long veterinarioId,
        String veterinarioNombre,
        String nombreVacuna,
        LocalDate fechaAplicacion,
        LocalDate proximaFecha,
        String observaciones) {
}
