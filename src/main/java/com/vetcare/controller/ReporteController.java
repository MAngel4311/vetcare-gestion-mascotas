package com.vetcare.controller;

import com.vetcare.dto.ConsultaReporteResponse;
import com.vetcare.dto.ResumenReporteResponse;
import com.vetcare.model.enums.EstadoCita;
import com.vetcare.service.ReporteService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** F-07: reportes (solo ADMINISTRADOR). */
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    /** Detalle. Ejemplo: GET /api/reportes/consultas?desde=2026-11-01&hasta=2026-11-30&veterinarioId=1 */
    @GetMapping("/consultas")
    public List<ConsultaReporteResponse> consultas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long veterinarioId,
            @RequestParam(required = false) EstadoCita estado) {
        return reporteService.consultas(desde, hasta, veterinarioId, estado);
    }

    /** RF-15: el mismo detalle en CSV (se abre directo en Excel). */
    @GetMapping(value = "/consultas/csv", produces = "text/csv")
    public ResponseEntity<byte[]> consultasCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long veterinarioId,
            @RequestParam(required = false) EstadoCita estado) {
        StringBuilder csv = new StringBuilder("\uFEFFcitaId;fecha;mascota;veterinario;motivo;estado\n");
        for (ConsultaReporteResponse f : reporteService.consultas(desde, hasta, veterinarioId, estado)) {
            csv.append(f.citaId()).append(';').append(f.fecha()).append(';')
                    .append(celda(f.mascota())).append(';').append(celda(f.veterinario())).append(';')
                    .append(f.motivo()).append(';').append(f.estado()).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-consultas.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Protege el CSV: comillas dobles y sin fórmulas (=, +, -, @) al abrirlo en Excel. */
    private static String celda(String texto) {
        String t = (texto == null) ? "" : texto.replace("\"", "\"\"");
        if (!t.isEmpty() && "=+-@".indexOf(t.charAt(0)) >= 0) {
            t = "'" + t;
        }
        return "\"" + t + "\"";
    }

    /** Consolidado (totales). Ejemplo: GET /api/reportes/resumen?desde=2026-11-01&hasta=2026-11-30 */
    @GetMapping("/resumen")
    public ResumenReporteResponse resumen(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long veterinarioId,
            @RequestParam(required = false) EstadoCita estado) {
        return reporteService.resumen(desde, hasta, veterinarioId, estado);
    }
}
