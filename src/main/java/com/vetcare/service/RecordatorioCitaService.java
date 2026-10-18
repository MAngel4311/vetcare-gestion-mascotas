package com.vetcare.service;

import com.vetcare.model.Cita;
import com.vetcare.repository.CitaRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recordatorio automático de la cita: cada 15 minutos busca las citas vigentes que empiezan en las próximas
 * horas (por defecto 24) y avisa al dueño por correo, una sola vez por cita.
 */
@Service
@Slf4j
public class RecordatorioCitaService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final CitaRepository citaRepository;
    private final NotificacionService notificacionService;
    private final int horas;
    private final boolean habilitado;

    public RecordatorioCitaService(CitaRepository citaRepository,
                                   NotificacionService notificacionService,
                                   @Value("${vetcare.citas.recordatorio-horas:24}") int horas,
                                   @Value("${vetcare.citas.recordatorio-habilitado:true}") boolean habilitado) {
        this.citaRepository = citaRepository;
        this.notificacionService = notificacionService;
        this.horas = horas;
        this.habilitado = habilitado;
    }

    @Scheduled(fixedDelayString = "${vetcare.citas.recordatorio-cada-ms:900000}", initialDelay = 60000)
    @Transactional
    public void programado() {
        if (!habilitado || !notificacionService.correoConfigurado()) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now(ZONA);
        List<Cita> citas = citaRepository.findParaRecordar(ahora, ahora.plusHours(horas));
        for (Cita c : citas) {
            String email = c.getMascota().getDueno().getEmail();
            if (email == null || email.isBlank()) {
                c.setRecordatorioEnviado(true); // sin correo no hay a quién avisar: no se reintenta cada 15 minutos
                citaRepository.save(c);
                continue;
            }
            String error = notificacionService.enviar(email,
                    "Recordatorio: cita de " + c.getMascota().getNombre() + " - Vet-Care",
                    "Hola " + c.getMascota().getDueno().getNombre() + ",\n\n"
                            + "Te recordamos la cita de " + c.getMascota().getNombre() + ":\n\n"
                            + "  Fecha: " + c.getFechaHoraInicio().toLocalDate() + "\n"
                            + "  Hora: " + c.getFechaHoraInicio().toLocalTime() + "\n"
                            + "  Veterinario: " + c.getVeterinario().getNombre() + "\n\n"
                            + "Si no puedes asistir, avísanos para reprogramarla.\n\nVet-Care");
            if (error == null) {
                c.setRecordatorioEnviado(true);
                citaRepository.save(c);
            }
        }
        if (!citas.isEmpty()) {
            log.info("Recordatorios de cita revisados: {}", citas.size());
        }
    }
}
