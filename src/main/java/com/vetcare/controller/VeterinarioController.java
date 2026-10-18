package com.vetcare.controller;

import com.vetcare.dto.DisponibilidadResponse;
import com.vetcare.dto.VeterinarioRequest;
import com.vetcare.dto.VeterinarioResponse;
import com.vetcare.service.DisponibilidadService;
import com.vetcare.service.VeterinarioService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Veterinarios (apoyo para citas, historial y reporte). */
@RestController
@RequestMapping("/api/veterinarios")
@RequiredArgsConstructor
public class VeterinarioController {

    private final VeterinarioService veterinarioService;
    private final DisponibilidadService disponibilidadService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VeterinarioResponse crear(@Valid @RequestBody VeterinarioRequest req) {
        return veterinarioService.crear(req);
    }

    @GetMapping
    public List<VeterinarioResponse> listar(@RequestParam(required = false) Boolean activo) {
        return veterinarioService.listar(activo);
    }

    @GetMapping("/{id}")
    public VeterinarioResponse obtener(@PathVariable Long id) {
        return veterinarioService.obtener(id);
    }

    @PutMapping("/{id}")
    public VeterinarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody VeterinarioRequest req) {
        return veterinarioService.actualizar(id, req);
    }

    /** Horas ocupadas y libres de un día. Ejemplo: GET /api/veterinarios/1/disponibilidad?fecha=2026-11-10&duracionMinutos=30 */
    @GetMapping("/{id}/disponibilidad")
    public DisponibilidadResponse disponibilidad(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Integer duracionMinutos) {
        return disponibilidadService.consultar(id, fecha, duracionMinutos);
    }
}
