package com.vetcare.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MascotaResponse(
        Long id,
        String nombre,
        String especie,
        String raza,
        LocalDate fechaNacimiento,
        BigDecimal pesoKg,
        Long duenoId,
        String duenoNombre,
        boolean activo) {
}
