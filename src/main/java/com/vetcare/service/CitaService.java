package com.vetcare.service;

import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import com.vetcare.security.UsuarioActual;
import static com.vetcare.mapper.EntityMapper.toResponse;

import com.vetcare.dto.CancelarCitaRequest;
import com.vetcare.dto.CitaRequest;
import com.vetcare.dto.CitaResponse;
import com.vetcare.dto.ReprogramarCitaRequest;
import com.vetcare.event.CitaProgramadaEvent;
import com.vetcare.event.CorreoEvent;
import com.vetcare.exception.BusinessException;
import com.vetcare.exception.ConflictException;
import com.vetcare.exception.NotFoundException;
import com.vetcare.model.Cita;
import com.vetcare.model.Mascota;
import com.vetcare.model.Veterinario;
import com.vetcare.model.enums.EstadoCita;
import com.vetcare.repository.CitaRepository;
import com.vetcare.repository.CitaSpecs;
import com.vetcare.repository.MascotaRepository;
import com.vetcare.repository.VeterinarioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F-02 (programar y cancelar) y F-06 (reprogramar). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CitaService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final String MSG_CHOQUE = "El veterinario ya tiene una cita en ese horario";

    private final CitaRepository citaRepository;
    private final MascotaRepository mascotaRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UsuarioActual usuarioActual;
    private final HorarioAtencion horario;

    /** SWR-02, SWR-03 y SWR-04. */
    @Transactional
    public CitaResponse programar(CitaRequest req) {
        validarHorario(req.fechaHoraInicio(), req.fechaHoraFin());

        Mascota mascota = mascotaRepository.findById(req.mascotaId())
                .orElseThrow(() -> new NotFoundException("Mascota no encontrada (id=" + req.mascotaId() + ")"));
        if (!mascota.isActivo()) {
            throw new ConflictException("La mascota " + mascota.getNombre() + " está dada de baja y no admite citas");
        }
        Veterinario veterinario = bloquearVeterinario(req.veterinarioId());

        if (citaRepository.existeChoque(veterinario.getId(), req.fechaHoraInicio(), req.fechaHoraFin())) {
            throw new ConflictException(MSG_CHOQUE);
        }

        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setVeterinario(veterinario);
        cita.setFechaHoraInicio(req.fechaHoraInicio());
        cita.setFechaHoraFin(req.fechaHoraFin());
        cita.setMotivo(req.motivo());
        cita.setEstado(EstadoCita.PROGRAMADA);
        cita = citaRepository.save(cita);

        // El correo se envía solo cuando la transacción termina bien (ver NotificacionService)
        eventPublisher.publishEvent(new CitaProgramadaEvent(
                mascota.getDueno().getEmail(),
                mascota.getDueno().getNombre(),
                mascota.getNombre(),
                veterinario.getNombre(),
                cita.getFechaHoraInicio(),
                cita.getFechaHoraFin(),
                cita.getMotivo()));

        return toResponse(cita);
    }

    /** RF-08: una cita PROGRAMADA (pendiente) pasa a CONFIRMADA. Solo las confirmadas se atienden. */
    @Transactional
    public CitaResponse confirmar(Long id) {
        Cita cita = buscar(id);
        if (cita.getEstado() != EstadoCita.PROGRAMADA) {
            throw new ConflictException(
                    "Solo se pueden confirmar citas PROGRAMADAS (estado actual: " + cita.getEstado() + ")");
        }
        if (!cita.getFechaHoraInicio().isAfter(LocalDateTime.now(ZONA))) {
            throw new BusinessException("No se puede confirmar una cita cuya fecha y hora ya pasaron");
        }
        cita.setEstado(EstadoCita.CONFIRMADA);
        avisar(cita, "Cita confirmada", "La clínica confirmó la cita. Te esperamos.");
        return toResponse(citaRepository.save(cita));
    }

    /**
     * SWR-05 y RF-09: se cancelan citas PROGRAMADAS o CONFIRMADAS cuya fecha aún no ha llegado,
     * y siempre con un motivo. Una cita ATENDIDA nunca se cancela.
     */
    @Transactional
    public CitaResponse cancelar(Long id, CancelarCitaRequest req) {
        Cita cita = buscar(id);
        if (!EstadoCita.VIGENTES.contains(cita.getEstado())) {
            throw new ConflictException(
                    "Solo se pueden cancelar citas PROGRAMADAS o CONFIRMADAS (estado actual: " + cita.getEstado() + ")");
        }
        if (!cita.getFechaHoraInicio().isAfter(LocalDateTime.now(ZONA))) {
            throw new BusinessException("No se puede cancelar una cita cuya fecha y hora ya pasaron");
        }
        cita.setEstado(EstadoCita.CANCELADA);
        cita.setMotivoCancelacion(req.motivo().trim());
        avisar(cita, "Cita cancelada", "La cita fue cancelada. Motivo: " + req.motivo().trim()
                + ".\nSi quieres una nueva fecha, comunícate con la clínica.");
        return toResponse(citaRepository.save(cita));
    }

    /** SWR-11: se vuelve a validar la disponibilidad del veterinario con el nuevo horario. */
    @Transactional
    public CitaResponse reprogramar(Long id, ReprogramarCitaRequest req) {
        Cita cita = buscar(id);
        if (!EstadoCita.VIGENTES.contains(cita.getEstado())) {
            throw new ConflictException(
                    "Solo se pueden reprogramar citas PROGRAMADAS o CONFIRMADAS (estado actual: " + cita.getEstado() + ")");
        }
        validarHorario(req.fechaHoraInicio(), req.fechaHoraFin());

        Veterinario veterinario = bloquearVeterinario(cita.getVeterinario().getId());
        if (citaRepository.existeChoqueExcluyendo(
                veterinario.getId(), cita.getId(), req.fechaHoraInicio(), req.fechaHoraFin())) {
            throw new ConflictException(MSG_CHOQUE);
        }

        cita.setFechaHoraInicio(req.fechaHoraInicio());
        cita.setFechaHoraFin(req.fechaHoraFin());
        cita.setRecordatorioEnviado(false); // la fecha cambió: el recordatorio previo vuelve a aplicar
        avisar(cita, "Cita reprogramada", "La cita fue reprogramada para la nueva fecha indicada abajo.");
        return toResponse(citaRepository.save(cita));
    }

    public CitaResponse obtener(Long id) {
        Cita cita = buscar(id);
        usuarioActual.exigirDueno(cita.getMascota().getDueno().getId(), "Cita no encontrada (id=" + id + ")");
        usuarioActual.exigirVeterinario(cita.getVeterinario().getId(), "Cita no encontrada (id=" + id + ")");
        return toResponse(cita);
    }

    /** Agenda: todos los filtros son opcionales. "fecha" trae las citas de ese día. */
    public List<CitaResponse> listar(LocalDate fecha, Long veterinarioId, Long mascotaId, EstadoCita estado) {
        LocalDateTime desde = (fecha != null) ? fecha.atStartOfDay() : null;
        LocalDateTime hasta = (fecha != null) ? fecha.plusDays(1).atStartOfDay() : null;
        Specification<Cita> filtro = CitaSpecs.filtrar(desde, hasta, veterinarioId, mascotaId, estado);
        Optional<Long> propio = usuarioActual.duenoRestringido();
        if (propio.isPresent()) {
            filtro = filtro.and(CitaSpecs.delDueno(propio.get()));
        }
        Optional<Long> vet = usuarioActual.veterinarioRestringido();
        if (vet.isPresent()) {
            filtro = filtro.and(CitaSpecs.delVeterinario(vet.get()));
        }
        return citaRepository
                .findAll(filtro, Sort.by("fechaHoraInicio"))
                .stream()
                .map(c -> toResponse(c))
                .toList();
    }

    /** Correo al dueño (se envía al terminar la transacción) con los datos actuales de la cita. */
    private void avisar(Cita cita, String asunto, String mensaje) {
        eventPublisher.publishEvent(new CorreoEvent(
                cita.getMascota().getDueno().getEmail(),
                asunto + " de " + cita.getMascota().getNombre() + " - Vet-Care",
                "Hola " + cita.getMascota().getDueno().getNombre() + ",\n\n" + mensaje + "\n\n"
                        + "  Mascota: " + cita.getMascota().getNombre() + "\n"
                        + "  Fecha: " + cita.getFechaHoraInicio().toLocalDate() + "\n"
                        + "  Hora: " + cita.getFechaHoraInicio().toLocalTime() + " a "
                        + cita.getFechaHoraFin().toLocalTime() + "\n"
                        + "  Veterinario: " + cita.getVeterinario().getNombre() + "\n\nVet-Care"));
    }

    private Cita buscar(Long id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada (id=" + id + ")"));
    }

    private Veterinario bloquearVeterinario(Long id) {
        Veterinario veterinario = veterinarioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Veterinario no encontrado (id=" + id + ")"));
        if (!veterinario.isActivo()) {
            throw new ConflictException("El veterinario " + veterinario.getNombre() + " está inactivo");
        }
        return veterinario;
    }

    /** RN-03: las reglas del horario viven en HorarioAtencion. */
    private void validarHorario(LocalDateTime inicio, LocalDateTime fin) {
        horario.validarCita(inicio, fin, LocalDateTime.now(ZONA));
    }
}
