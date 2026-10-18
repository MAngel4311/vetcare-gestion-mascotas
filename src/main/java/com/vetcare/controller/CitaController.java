package com.vetcare.controller;

import com.vetcare.dto.CancelarCitaRequest;
import com.vetcare.dto.CitaRequest;
import com.vetcare.dto.CitaResponse;
import com.vetcare.dto.RegistroClinicoRequest;
import com.vetcare.dto.RegistroClinicoResponse;
import com.vetcare.dto.ReprogramarCitaRequest;
import com.vetcare.model.enums.EstadoCita;
import com.vetcare.service.CitaService;
import com.vetcare.service.RegistroClinicoService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** F-02 (programar, cancelar), F-06 (reprogramar) y F-03 (registrar consulta). */
@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;
    private final RegistroClinicoService registroClinicoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CitaResponse programar(@Valid @RequestBody CitaRequest req) {
        return citaService.programar(req);
    }

    /** Agenda. Ejemplo: GET /api/citas?fecha=2026-11-10&veterinarioId=1 */
    @GetMapping
    public List<CitaResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long veterinarioId,
            @RequestParam(required = false) Long mascotaId,
            @RequestParam(required = false) EstadoCita estado) {
        return citaService.listar(fecha, veterinarioId, mascotaId, estado);
    }

    @GetMapping("/{id}")
    public CitaResponse obtener(@PathVariable Long id) {
        return citaService.obtener(id);
    }

    /** RF-08: pasa la cita de PROGRAMADA (pendiente) a CONFIRMADA. */
    @PatchMapping("/{id}/confirmar")
    public CitaResponse confirmar(@PathVariable Long id) {
        return citaService.confirmar(id);
    }

    /** SWR-05 y RF-09: el cuerpo lleva el motivo de la cancelación. */
    @PatchMapping("/{id}/cancelar")
    public CitaResponse cancelar(@PathVariable Long id, @Valid @RequestBody CancelarCitaRequest req) {
        return citaService.cancelar(id, req);
    }

    /** SWR-11 */
    @PutMapping("/{id}/reprogramar")
    public CitaResponse reprogramar(@PathVariable Long id, @Valid @RequestBody ReprogramarCitaRequest req) {
        return citaService.reprogramar(id, req);
    }

    /** SWR-06, SWR-07, SWR-08 */
    @PostMapping("/{id}/registro-clinico")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroClinicoResponse registrarConsulta(@PathVariable Long id,
                                                     @Valid @RequestBody RegistroClinicoRequest req) {
        return registroClinicoService.registrar(id, req);
    }
}
