package com.vetcare.service;

import com.vetcare.dto.BloqueOcupadoResponse;
import com.vetcare.dto.DisponibilidadResponse;
import com.vetcare.dto.RangoHorarioResponse;
import com.vetcare.exception.BusinessException;
import com.vetcare.exception.NotFoundException;
import com.vetcare.model.Cita;
import com.vetcare.model.Veterinario;
import com.vetcare.repository.CitaRepository;
import com.vetcare.repository.VeterinarioRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * F-02: la recepcionista agenda en una "hora disponible" (StR). Este servicio dice qué horas
 * están ocupadas y cuáles libres para un veterinario en un día. Es informativo: la validación
 * que realmente impide los choques sigue estando en CitaService.
 */
@Service
@Transactional(readOnly = true)
public class DisponibilidadService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final VeterinarioRepository veterinarioRepository;
    private final CitaRepository citaRepository;
    private final LocalTime apertura;
    private final LocalTime cierre;

    public DisponibilidadService(VeterinarioRepository veterinarioRepository,
                                 CitaRepository citaRepository,
                                 @Value("${vetcare.agenda.hora-apertura:08:00}") String apertura,
                                 @Value("${vetcare.agenda.hora-cierre:18:00}") String cierre) {
        this.veterinarioRepository = veterinarioRepository;
        this.citaRepository = citaRepository;
        this.apertura = LocalTime.parse(apertura);
        this.cierre = LocalTime.parse(cierre);
        if (!this.cierre.isAfter(this.apertura)) {
            throw new IllegalStateException("vetcare.agenda.hora-cierre debe ser posterior a hora-apertura");
        }
    }

    /** Si se envía duracionMinutos, solo se devuelven los huecos donde cabe una cita de esa duración. */
    public DisponibilidadResponse consultar(Long veterinarioId, LocalDate fecha, Integer duracionMinutos) {
        Veterinario veterinario = veterinarioRepository.findById(veterinarioId)
                .orElseThrow(() -> new NotFoundException("Veterinario no encontrado (id=" + veterinarioId + ")"));
        if (duracionMinutos != null && duracionMinutos <= 0) {
            throw new BusinessException("La duración debe ser mayor que 0 minutos");
        }

        List<Cita> citas = citaRepository.findProgramadasEnRango(
                veterinarioId, fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());

        List<BloqueOcupadoResponse> ocupados = citas.stream()
                .map(c -> new BloqueOcupadoResponse(c.getId(), c.getMascota().getNombre(),
                        c.getFechaHoraInicio(), c.getFechaHoraFin()))
                .toList();

        return new DisponibilidadResponse(veterinario.getId(), veterinario.getNombre(), fecha,
                apertura, cierre, ocupados, calcularLibres(fecha, citas, duracionMinutos));
    }

    /** Recorre las citas (ya ordenadas) y va anotando los huecos entre una y otra dentro del horario de atención. */
    private List<RangoHorarioResponse> calcularLibres(LocalDate fecha, List<Cita> citas, Integer duracionMinutos) {
        LocalDateTime ahora = LocalDateTime.now(ZONA).truncatedTo(ChronoUnit.MINUTES);
        LocalDate hoy = ahora.toLocalDate();
        if (fecha.isBefore(hoy)) {
            return List.of(); // no tiene sentido ofrecer horas libres en el pasado
        }

        LocalDateTime abre = fecha.atTime(apertura);
        LocalDateTime cierra = fecha.atTime(cierre);
        // Si es hoy y ya pasó la hora de apertura, lo libre empieza desde ahora
        LocalDateTime cursor = (fecha.equals(hoy) && ahora.isAfter(abre)) ? ahora : abre;

        List<RangoHorarioResponse> libres = new ArrayList<>();
        for (Cita cita : citas) {
            if (cita.getFechaHoraInicio().isAfter(cursor)) {
                LocalDateTime finHueco = cita.getFechaHoraInicio().isBefore(cierra)
                        ? cita.getFechaHoraInicio() : cierra;
                agregarSiCabe(libres, cursor, finHueco, duracionMinutos);
            }
            if (cita.getFechaHoraFin().isAfter(cursor)) {
                cursor = cita.getFechaHoraFin();
            }
        }
        if (cursor.isBefore(cierra)) {
            agregarSiCabe(libres, cursor, cierra, duracionMinutos);
        }
        return libres;
    }

    private void agregarSiCabe(List<RangoHorarioResponse> libres, LocalDateTime inicio, LocalDateTime fin,
                               Integer duracionMinutos) {
        if (!fin.isAfter(inicio)) {
            return;
        }
        if (duracionMinutos != null && Duration.between(inicio, fin).toMinutes() < duracionMinutos) {
            return;
        }
        libres.add(new RangoHorarioResponse(inicio, fin));
    }
}
