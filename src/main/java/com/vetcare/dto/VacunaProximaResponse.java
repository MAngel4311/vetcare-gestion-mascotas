package com.vetcare.dto;

import java.time.LocalDate;

/** Vacuna cuya siguiente dosis está pendiente. diasRestantes negativo = ya venció. */
public record VacunaProximaResponse(
        Long vacunacionId,
        Long mascotaId,
        String mascotaNombre,
        String duenoNombre,
        String duenoTelefono,
        String duenoEmail,
        String nombreVacuna,
        LocalDate fechaAplicacion,
        LocalDate proximaFecha,
        long diasRestantes) {
}
