package com.vetcare.controller;

import com.vetcare.dto.MascotaRequest;
import com.vetcare.dto.MascotaResponse;
import com.vetcare.dto.RegistroClinicoResponse;
import com.vetcare.dto.VacunacionRequest;
import com.vetcare.dto.VacunacionResponse;
import com.vetcare.service.MascotaService;
import com.vetcare.service.RegistroClinicoService;
import com.vetcare.service.VacunacionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

/** F-05 (mascotas), F-04 (historial) y F-08 (vacunas). */
@RestController
@RequestMapping("/api/mascotas")
@RequiredArgsConstructor
public class MascotaController {

    private final MascotaService mascotaService;
    private final RegistroClinicoService registroClinicoService;
    private final VacunacionService vacunacionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MascotaResponse crear(@Valid @RequestBody MascotaRequest req) {
        return mascotaService.crear(req);
    }

    @GetMapping
    public List<MascotaResponse> buscar(@RequestParam(required = false) String nombre,
                                        @RequestParam(required = false) Long duenoId,
                                        @RequestParam(required = false) String especie) {
        return mascotaService.buscar(nombre, duenoId, especie);
    }

    @GetMapping("/{id}")
    public MascotaResponse obtener(@PathVariable Long id) {
        return mascotaService.obtener(id);
    }

    @PutMapping("/{id}")
    public MascotaResponse actualizar(@PathVariable Long id, @Valid @RequestBody MascotaRequest req) {
        return mascotaService.actualizar(id, req);
    }

    /** F-04 / SWR-09 */
    @GetMapping("/{id}/historial")
    public List<RegistroClinicoResponse> historial(@PathVariable Long id) {
        return registroClinicoService.historial(id);
    }

    /** F-08 / SWR-13 */
    @PostMapping("/{id}/vacunas")
    @ResponseStatus(HttpStatus.CREATED)
    public VacunacionResponse registrarVacuna(@PathVariable Long id, @Valid @RequestBody VacunacionRequest req) {
        return vacunacionService.registrar(id, req);
    }

    @GetMapping("/{id}/vacunas")
    public List<VacunacionResponse> listarVacunas(@PathVariable Long id) {
        return vacunacionService.listar(id);
    }
}
