package com.vetcare.service;

import com.vetcare.event.CitaProgramadaEvent;
import com.vetcare.event.CorreoEvent;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envío de correos (SWR-04 y demás avisos al dueño).
 * Los correos de eventos se mandan DESPUÉS de guardar y en otro hilo: si el correo falla, el dato no se pierde.
 * Cualquier servicio que no esté dentro de una transacción (los recordatorios programados) usa enviar().
 */
@Service
@Slf4j
public class NotificacionService {

    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", ES);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String remitente;

    public NotificacionService(ObjectProvider<JavaMailSender> mailSenderProvider,
                               @Value("${vetcare.mail.from}") String remitente) {
        this.mailSenderProvider = mailSenderProvider;
        this.remitente = remitente;
    }

    /** true si hay un servidor de correo configurado (spring.mail.host / variable MAIL_HOST). */
    public boolean correoConfigurado() {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            return false;
        }
        return !(sender instanceof JavaMailSenderImpl impl)
                || (impl.getHost() != null && !impl.getHost().isBlank());
    }

    /**
     * Envía un correo ya. Devuelve null si salió bien, o el motivo si no se pudo enviar
     * (sin correo configurado, sin destinatario o error del servidor).
     */
    public String enviar(String para, String asunto, String texto) {
        if (para == null || para.isBlank()) {
            return "El destinatario no tiene email registrado";
        }
        if (!correoConfigurado()) {
            log.info("Correo no configurado (MAIL_HOST). No se envió «{}» a {}", asunto, para);
            return "Correo no configurado: define MAIL_HOST, MAIL_USER y MAIL_PASSWORD";
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(para);
            mensaje.setSubject(asunto);
            mensaje.setText(texto);
            mailSenderProvider.getObject().send(mensaje);
            log.info("Correo «{}» enviado a {}", asunto, para);
            return null;
        } catch (Exception ex) {
            log.error("No se pudo enviar «{}» a {}", asunto, para, ex);
            return "Error del servidor de correo: " + ex.getMessage();
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviarCorreo(CorreoEvent evento) {
        enviar(evento.para(), evento.asunto(), evento.texto());
    }

    /** SWR-04: confirmación de la cita al dueño. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviarConfirmacion(CitaProgramadaEvent e) {
        enviar(e.emailDueno(), "Cita registrada para " + e.nombreMascota() + " - Vet-Care",
                "Hola " + e.nombreDueno() + ",\n\n"
                        + "La cita de " + e.nombreMascota() + " quedó registrada en Vet-Care:\n\n"
                        + "  Fecha: " + FECHA.format(e.inicio()) + "\n"
                        + "  Hora: " + HORA.format(e.inicio()) + " a " + HORA.format(e.fin()) + "\n"
                        + "  Veterinario: " + e.nombreVeterinario() + "\n"
                        + "  Motivo: " + e.motivo().name().toLowerCase(ES) + "\n\n"
                        + "Te avisaremos cuando la clínica la confirme. Si necesitas cancelarla o reprogramarla, "
                        + "comunícate con la clínica.\n\nVet-Care");
    }
}
