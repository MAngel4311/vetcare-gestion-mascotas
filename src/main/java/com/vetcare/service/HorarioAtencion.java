package com.vetcare.service;

import com.vetcare.exception.BusinessException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RN-03: horario de atención de la clínica (lunes a sábado, entre apertura y cierre).
 * Es la única fuente de la configuración {@code vetcare.agenda.*}: la usan CitaService
 * (para validar) y DisponibilidadService (para calcular los huecos libres).
 */
@Component
public class HorarioAtencion {

    private final LocalTime apertura;
    private final LocalTime cierre;

    public HorarioAtencion(@Value("${vetcare.agenda.hora-apertura:08:00}") String apertura,
                           @Value("${vetcare.agenda.hora-cierre:18:00}") String cierre) {
        this.apertura = LocalTime.parse(apertura);
        this.cierre = LocalTime.parse(cierre);
        if (!this.cierre.isAfter(this.apertura)) {
            throw new IllegalStateException("vetcare.agenda.hora-cierre debe ser posterior a hora-apertura");
        }
    }

    public LocalTime apertura() {
        return apertura;
    }

    public LocalTime cierre() {
        return cierre;
    }

    /**
     * Valida que [inicio, fin) sea una cita agendable. "ahora" se recibe como parámetro
     * para poder probar la regla sin depender del reloj.
     */
    public void validarCita(LocalDateTime inicio, LocalDateTime fin, LocalDateTime ahora) {
        if (!fin.isAfter(inicio)) {
            throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio");
        }
        if (!inicio.isAfter(ahora)) {
            throw new BusinessException("La cita debe programarse en una fecha y hora futuras");
        }
        if (!inicio.toLocalDate().equals(fin.toLocalDate())) {
            throw new BusinessException("La cita debe empezar y terminar el mismo día");
        }
        if (inicio.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new BusinessException("La clínica no atiende los domingos (horario: lunes a sábado)");
        }
        if (inicio.toLocalTime().isBefore(apertura) || fin.toLocalTime().isAfter(cierre)) {
            throw new BusinessException("La cita debe estar dentro del horario de atención ("
                    + apertura + " a " + cierre + ", lunes a sábado)");
        }
    }
}
