package com.vetcare.dto;

/** Resultado de una corrida del recordatorio de vacunas (RF-13). */
public record RecordatorioVacunasResponse(int pendientes, int enviados, int sinCorreo) {
}
