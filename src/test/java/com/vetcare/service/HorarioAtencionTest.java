package com.vetcare.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vetcare.exception.BusinessException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** RN-03. Pruebas de caracterización: fijan el comportamiento que tenía CitaService.validarHorario. */
class HorarioAtencionTest {

    private final HorarioAtencion horario = new HorarioAtencion("08:00", "18:00");
    // Domingo 2026-11-01 12:00 como "ahora": todas las fechas de prueba son posteriores
    private final LocalDateTime ahora = LocalDateTime.of(2026, 11, 1, 12, 0);

    private String mensaje(LocalDateTime ini, LocalDateTime fin) {
        return assertThrows(BusinessException.class, () -> horario.validarCita(ini, fin, ahora)).getMessage();
    }

    @Test
    void aceptaCitaDentroDelHorarioEnDiaHabil() {
        assertDoesNotThrow(() -> horario.validarCita(
                LocalDateTime.of(2026, 11, 2, 9, 0), LocalDateTime.of(2026, 11, 2, 9, 30), ahora)); // lunes
    }

    @Test
    void aceptaLosLimitesExactosDelHorario() {
        assertDoesNotThrow(() -> horario.validarCita(
                LocalDateTime.of(2026, 11, 2, 8, 0), LocalDateTime.of(2026, 11, 2, 18, 0), ahora));
    }

    @Test
    void rechazaFinNoPosteriorAlInicio() {
        assertEquals("La hora de fin debe ser posterior a la hora de inicio",
                mensaje(LocalDateTime.of(2026, 11, 2, 9, 0), LocalDateTime.of(2026, 11, 2, 9, 0)));
    }

    @Test
    void rechazaCitaEnElPasado() {
        assertEquals("La cita debe programarse en una fecha y hora futuras",
                mensaje(LocalDateTime.of(2026, 11, 1, 11, 0), LocalDateTime.of(2026, 11, 1, 11, 30)));
    }

    @Test
    void rechazaCitaQueCruzaDeDia() {
        assertEquals("La cita debe empezar y terminar el mismo día",
                mensaje(LocalDateTime.of(2026, 11, 2, 17, 0), LocalDateTime.of(2026, 11, 3, 9, 0)));
    }

    @Test
    void rechazaDomingos() {
        assertEquals("La clínica no atiende los domingos (horario: lunes a sábado)",
                mensaje(LocalDateTime.of(2026, 11, 8, 9, 0), LocalDateTime.of(2026, 11, 8, 9, 30)));
    }

    @Test
    void rechazaFueraDeHorario() {
        String esperado = "La cita debe estar dentro del horario de atención (08:00 a 18:00, lunes a sábado)";
        assertEquals(esperado, mensaje(LocalDateTime.of(2026, 11, 2, 7, 30), LocalDateTime.of(2026, 11, 2, 8, 30)));
        assertEquals(esperado, mensaje(LocalDateTime.of(2026, 11, 2, 17, 45), LocalDateTime.of(2026, 11, 2, 18, 15)));
    }

    @Test
    void aceptaSabado() {
        assertDoesNotThrow(() -> horario.validarCita(
                LocalDateTime.of(2026, 11, 7, 10, 0), LocalDateTime.of(2026, 11, 7, 10, 30), ahora));
    }

    @Test
    void configuracionInvalidaFallaAlArrancar() {
        assertThrows(IllegalStateException.class, () -> new HorarioAtencion("18:00", "08:00"));
    }
}
