package com.vetcare.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DisponibilidadResponse(
        Long veterinarioId,
        String veterinarioNombre,
        LocalDate fecha,
        LocalTime horaApertura,
        LocalTime horaCierre,
        List<BloqueOcupadoResponse> ocupados,
        List<RangoHorarioResponse> libres) {
}
