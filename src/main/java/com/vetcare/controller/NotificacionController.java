package com.vetcare.controller;

import com.vetcare.dto.PruebaCorreoResponse;
import com.vetcare.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Utilidad para el administrador: comprobar que el correo está bien configurado. */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    /** Ejemplo: POST /api/notificaciones/prueba?para=tucorreo@gmail.com */
    @PostMapping("/prueba")
    public PruebaCorreoResponse prueba(@RequestParam String para) {
        String error = notificacionService.enviar(para, "Prueba de correo - Vet-Care",
                "Si recibes este mensaje, el correo de Vet-Care está bien configurado.\n\nVet-Care");
        return new PruebaCorreoResponse(error == null, notificacionService.correoConfigurado(),
                error == null ? "Correo enviado a " + para : error);
    }
}
