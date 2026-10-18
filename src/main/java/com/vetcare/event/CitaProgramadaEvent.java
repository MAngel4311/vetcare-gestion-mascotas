package com.vetcare.event;

import com.vetcare.model.enums.MotivoCita;
import java.time.LocalDateTime;

/** Se publica al agendar una cita; dispara el correo de confirmación (SWR-04). */
public record CitaProgramadaEvent(
        String emailDueno,
        String nombreDueno,
        String nombreMascota,
        String nombreVeterinario,
        LocalDateTime inicio,
        LocalDateTime fin,
        MotivoCita motivo) {
}
