package com.vetcare.dto;

import java.time.LocalDateTime;

/** Un hueco libre en la agenda: desde "inicio" hasta "fin". */
public record RangoHorarioResponse(LocalDateTime inicio, LocalDateTime fin) {
}
