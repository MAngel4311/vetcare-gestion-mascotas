package com.vetcare.service;

import static com.vetcare.mapper.EntityMapper.toReporte;

import com.vetcare.dto.ConsultaReporteResponse;
import com.vetcare.dto.ConteoMotivoResponse;
import com.vetcare.dto.ConteoVeterinarioResponse;
import com.vetcare.dto.ResumenReporteResponse;
import com.vetcare.exception.BusinessException;
import com.vetcare.model.Cita;
import com.vetcare.model.enums.EstadoCita;
import com.vetcare.model.enums.MotivoCita;
import com.vetcare.repository.CitaRepository;
import com.vetcare.repository.CitaSpecs;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F-07 / SWR-12: reporte de consultas por periodo y, opcionalmente, por veterinario. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteService {

    private final CitaRepository citaRepository;

    /**
     * Detalle fila por fila. El periodo es inclusivo en ambos extremos. Por defecto solo cuenta
     * las consultas ATENDIDAS; si se envía "estado" se puede ver otro estado (por ejemplo CANCELADA).
     */
    public List<ConsultaReporteResponse> consultas(LocalDate desde, LocalDate hasta,
                                                   Long veterinarioId, EstadoCita estado) {
        return buscarCitas(desde, hasta, veterinarioId, estado).stream()
                .map(c -> toReporte(c))
                .toList();
    }

    /** Versión consolidada: total de consultas y cuántas por veterinario y por motivo. */
    public ResumenReporteResponse resumen(LocalDate desde, LocalDate hasta,
                                          Long veterinarioId, EstadoCita estado) {
        List<Cita> citas = buscarCitas(desde, hasta, veterinarioId, estado);

        Map<Long, String> nombres = new LinkedHashMap<>();
        Map<Long, Long> porVeterinario = new LinkedHashMap<>();
        Map<MotivoCita, Long> porMotivo = new EnumMap<>(MotivoCita.class);
        for (Cita cita : citas) {
            Long veterinarioCita = cita.getVeterinario().getId();
            nombres.putIfAbsent(veterinarioCita, cita.getVeterinario().getNombre());
            porVeterinario.merge(veterinarioCita, 1L, Long::sum);
            porMotivo.merge(cita.getMotivo(), 1L, Long::sum);
        }

        Comparator<ConteoVeterinarioResponse> masConsultasPrimero =
                Comparator.comparingLong(ConteoVeterinarioResponse::total).reversed();
        List<ConteoVeterinarioResponse> listaVeterinarios = porVeterinario.entrySet().stream()
                .map(e -> new ConteoVeterinarioResponse(e.getKey(), nombres.get(e.getKey()), e.getValue()))
                .sorted(masConsultasPrimero)
                .toList();

        List<ConteoMotivoResponse> listaMotivos = porMotivo.entrySet().stream()
                .map(e -> new ConteoMotivoResponse(e.getKey(), e.getValue()))
                .toList();

        EstadoCita estadoUsado = (estado != null) ? estado : EstadoCita.ATENDIDA;
        return new ResumenReporteResponse(desde, hasta, estadoUsado, veterinarioId,
                citas.size(), listaVeterinarios, listaMotivos);
    }

    private List<Cita> buscarCitas(LocalDate desde, LocalDate hasta, Long veterinarioId, EstadoCita estado) {
        if (hasta.isBefore(desde)) {
            throw new BusinessException("La fecha 'hasta' no puede ser anterior a 'desde'");
        }
        EstadoCita filtroEstado = (estado != null) ? estado : EstadoCita.ATENDIDA;
        return citaRepository.findAll(
                CitaSpecs.filtrar(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay(),
                        veterinarioId, null, filtroEstado),
                Sort.by("fechaHoraInicio"));
    }
}
