package com.vetcare.service;

import com.vetcare.dto.RecordatorioVacunasResponse;
import com.vetcare.model.Dueno;
import com.vetcare.model.Vacunacion;
import com.vetcare.repository.VacunacionRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-13: correo de recordatorio cuando una vacuna está por vencer.
 * Corre solo todos los días a la hora configurada y también al arrancar la aplicación (por si el servidor
 * estuvo apagado a esa hora). Cada dosis se avisa una sola vez, así que repetirlo no manda correos duplicados.
 */
@Service
@Slf4j
public class RecordatorioVacunaService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"));

    private final VacunacionRepository vacunacionRepository;
    private final NotificacionService notificacionService;
    private final int margenDias;
    private final boolean habilitado;

    public RecordatorioVacunaService(VacunacionRepository vacunacionRepository,
                                     NotificacionService notificacionService,
                                     @Value("${vetcare.vacunas.recordatorio-dias:7}") int margenDias,
                                     @Value("${vetcare.vacunas.recordatorio-habilitado:true}") boolean habilitado) {
        this.vacunacionRepository = vacunacionRepository;
        this.notificacionService = notificacionService;
        this.margenDias = margenDias;
        this.habilitado = habilitado;
    }

    @Scheduled(cron = "${vetcare.vacunas.recordatorio-cron:0 0 8 * * *}", zone = "America/Bogota")
    @Transactional
    public void programado() {
        if (habilitado) {
            enviarRecordatorios();
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void alIniciar() {
        programado();
    }

    /** También se puede lanzar a mano (POST /api/vacunas/recordatorios) para probar. */
    @Transactional
    public RecordatorioVacunasResponse enviarRecordatorios() {
        LocalDate hoy = LocalDate.now(ZONA);
        List<Vacunacion> pendientes = vacunacionRepository.findParaRecordar(hoy, hoy.plusDays(margenDias));
        if (!notificacionService.correoConfigurado()) {
            log.info("Correo no configurado: {} recordatorios de vacuna quedan pendientes", pendientes.size());
            return new RecordatorioVacunasResponse(pendientes.size(), 0, 0);
        }

        int enviados = 0;
        int sinCorreo = 0;
        for (Vacunacion v : pendientes) {
            Dueno dueno = v.getMascota().getDueno();
            if (dueno.getEmail() == null || dueno.getEmail().isBlank()) {
                sinCorreo++;
                continue;
            }
            String error = notificacionService.enviar(dueno.getEmail(),
                    "Recordatorio de vacuna para " + v.getMascota().getNombre() + " - Vet-Care",
                    "Hola " + dueno.getNombre() + ",\n\n"
                            + "A " + v.getMascota().getNombre() + " le corresponde la próxima dosis de "
                            + v.getNombreVacuna() + " el " + FECHA.format(v.getProximaFecha()) + ".\n"
                            + "Comunícate con la clínica para agendar la cita.\n\nVet-Care");
            if (error == null) {
                v.setRecordatorioEnviado(true);
                vacunacionRepository.save(v);
                enviados++;
            }
        }
        log.info("Recordatorios de vacuna: {} enviados, {} sin correo, {} pendientes",
                enviados, sinCorreo, pendientes.size());
        return new RecordatorioVacunasResponse(pendientes.size(), enviados, sinCorreo);
    }
}
