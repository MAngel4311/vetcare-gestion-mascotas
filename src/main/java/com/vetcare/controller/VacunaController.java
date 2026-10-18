package com.vetcare.controller;

import com.vetcare.dto.VacunaProximaResponse;
import com.vetcare.dto.RecordatorioVacunasResponse;
import com.vetcare.service.RecordatorioVacunaService;
import com.vetcare.service.VacunacionService;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** F-08: seguimiento de vacunas de todas las mascotas. */
@RestController
@RequestMapping("/api/vacunas")
@RequiredArgsConstructor
public class VacunaController {

    private final VacunacionService vacunacionService;
    private final RecordatorioVacunaService recordatorioVacunaService;

    /** RF-13: dispara ya el envío de recordatorios (normalmente corre solo cada día). Solo ADMINISTRADOR. */
    @PostMapping("/recordatorios")
    public RecordatorioVacunasResponse enviarRecordatorios() {
        return recordatorioVacunaService.enviarRecordatorios();
    }

    /** Ejemplo: GET /api/vacunas/proximas?dias=30&incluirVencidas=true */
    @GetMapping("/proximas")
    public List<VacunaProximaResponse> proximas(
            @RequestParam(defaultValue = "30") int dias,
            @RequestParam(defaultValue = "false") boolean incluirVencidas) {
        return vacunacionService.proximas(dias, incluirVencidas);
    }
}
