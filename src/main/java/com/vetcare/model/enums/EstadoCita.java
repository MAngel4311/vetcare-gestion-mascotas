package com.vetcare.model.enums;

import java.util.List;

/**
 * Estado de la cita: PROGRAMADA (pendiente de confirmar) -> CONFIRMADA -> ATENDIDA, o CANCELADA.
 * Las citas PROGRAMADA y CONFIRMADA ocupan tiempo del veterinario (SWR-02 y SWR-11).
 */
public enum EstadoCita {
    PROGRAMADA,
    CONFIRMADA,
    CANCELADA,
    ATENDIDA;

    /** Estados que todavía están vigentes (se pueden cancelar o reprogramar). */
    public static final List<EstadoCita> VIGENTES = List.of(PROGRAMADA, CONFIRMADA);
}
