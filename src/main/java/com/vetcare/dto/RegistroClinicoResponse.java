package com.vetcare.dto;

import com.vetcare.model.enums.MotivoCita;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RegistroClinicoResponse(
        Long id,
        Long citaId,
        MotivoCita motivo,
        Long mascotaId,
        String mascotaNombre,
        Long veterinarioId,
        String veterinarioNombre,
        LocalDateTime fechaConsulta,
        String diagnostico,
        String tratamiento,
        String observaciones,
        BigDecimal pesoKg,
        BigDecimal temperaturaC,
        String alergias) {
}
