package com.vetcare.service;

import com.vetcare.security.UsuarioActual;
import static com.vetcare.mapper.EntityMapper.toResponse;

import com.vetcare.dto.VacunacionRequest;
import com.vetcare.dto.VacunaProximaResponse;
import com.vetcare.dto.VacunacionResponse;
import com.vetcare.exception.BusinessException;
import com.vetcare.exception.NotFoundException;
import com.vetcare.model.Dueno;
import com.vetcare.model.Mascota;
import com.vetcare.model.Vacunacion;
import com.vetcare.model.Veterinario;
import com.vetcare.repository.MascotaRepository;
import com.vetcare.repository.VacunacionRepository;
import com.vetcare.repository.VeterinarioRepository;
import com.vetcare.util.Textos;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F-08: registrar y consultar vacunaciones de una mascota. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VacunacionService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final VacunacionRepository vacunacionRepository;
    private final MascotaRepository mascotaRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final UsuarioActual usuarioActual;

    /** SWR-13. */
    @Transactional
    public VacunacionResponse registrar(Long mascotaId, VacunacionRequest req) {
        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new NotFoundException("Mascota no encontrada (id=" + mascotaId + ")"));

        if (req.proximaFecha() != null && !req.proximaFecha().isAfter(req.fechaAplicacion())) {
            throw new BusinessException("La próxima fecha debe ser posterior a la fecha de aplicación");
        }

        Veterinario veterinario = null;
        if (req.veterinarioId() != null) {
            veterinario = veterinarioRepository.findById(req.veterinarioId())
                    .orElseThrow(() -> new NotFoundException(
                            "Veterinario no encontrado (id=" + req.veterinarioId() + ")"));
        }

        Vacunacion vacunacion = new Vacunacion();
        vacunacion.setMascota(mascota);
        vacunacion.setVeterinario(veterinario);
        vacunacion.setNombreVacuna(req.nombreVacuna().trim());
        vacunacion.setFechaAplicacion(req.fechaAplicacion());
        vacunacion.setProximaFecha(req.proximaFecha());
        vacunacion.setObservaciones(Textos.limpiar(req.observaciones()));
        return toResponse(vacunacionRepository.save(vacunacion));
    }

    public List<VacunacionResponse> listar(Long mascotaId) {
        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new NotFoundException("Mascota no encontrada (id=" + mascotaId + ")"));
        usuarioActual.exigirMascotaPropia(mascota);
        return vacunacionRepository.findByMascotaIdOrderByFechaAplicacionDesc(mascotaId)
                .stream()
                .map(v -> toResponse(v))
                .toList();
    }

    /**
     * Seguimiento de vacunas (BR de F-08): próximas dosis dentro de los siguientes "dias" días.
     * Con incluirVencidas=true también trae las que ya pasaron sin volver a aplicarse.
     */
    public List<VacunaProximaResponse> proximas(int dias, boolean incluirVencidas) {
        if (dias < 1 || dias > 365) {
            throw new BusinessException("El parámetro 'dias' debe estar entre 1 y 365");
        }
        LocalDate hoy = LocalDate.now(ZONA);
        LocalDate desde = incluirVencidas ? LocalDate.of(1900, 1, 1) : hoy;
        return vacunacionRepository.findPendientes(desde, hoy.plusDays(dias)).stream()
                .map(v -> aProxima(v, hoy))
                .toList();
    }

    private VacunaProximaResponse aProxima(Vacunacion v, LocalDate hoy) {
        Mascota mascota = v.getMascota();
        Dueno dueno = mascota.getDueno();
        return new VacunaProximaResponse(v.getId(), mascota.getId(), mascota.getNombre(),
                dueno.getNombre(), dueno.getTelefono(), dueno.getEmail(),
                v.getNombreVacuna(), v.getFechaAplicacion(), v.getProximaFecha(),
                ChronoUnit.DAYS.between(hoy, v.getProximaFecha()));
    }
}
