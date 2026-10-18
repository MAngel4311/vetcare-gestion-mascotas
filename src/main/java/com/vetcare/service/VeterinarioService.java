package com.vetcare.service;

import static com.vetcare.mapper.EntityMapper.toResponse;

import com.vetcare.dto.VeterinarioRequest;
import com.vetcare.dto.VeterinarioResponse;
import com.vetcare.exception.ConflictException;
import com.vetcare.exception.NotFoundException;
import com.vetcare.model.Veterinario;
import com.vetcare.model.enums.EstadoCita;
import com.vetcare.repository.CitaRepository;
import com.vetcare.repository.VeterinarioRepository;
import com.vetcare.util.Textos;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestión básica de veterinarios (se necesitan para citas, historial y reporte). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VeterinarioService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final VeterinarioRepository veterinarioRepository;
    private final CitaRepository citaRepository;

    @Transactional
    public VeterinarioResponse crear(VeterinarioRequest req) {
        String documento = req.documento().trim();
        if (veterinarioRepository.existsByDocumento(documento)) {
            throw new ConflictException("Ya existe un veterinario con el documento " + documento);
        }
        Veterinario veterinario = new Veterinario();
        aplicar(veterinario, req);
        return toResponse(veterinarioRepository.save(veterinario));
    }

    @Transactional
    public VeterinarioResponse actualizar(Long id, VeterinarioRequest req) {
        Veterinario veterinario = buscar(id);
        String documento = req.documento().trim();
        if (veterinarioRepository.existsByDocumentoAndIdNot(documento, id)) {
            throw new ConflictException("Ya existe otro veterinario con el documento " + documento);
        }
        // RF-14 / RN-07: no se desactiva un veterinario que aún tiene citas por atender
        if (Boolean.FALSE.equals(req.activo()) && veterinario.isActivo()
                && citaRepository.existsByVeterinarioIdAndEstadoInAndFechaHoraInicioAfter(
                        id, EstadoCita.VIGENTES, LocalDateTime.now(ZONA))) {
            throw new ConflictException("No se puede desactivar: el veterinario tiene citas futuras "
                    + "programadas o confirmadas. Cancélalas o reprogramalas primero");
        }
        aplicar(veterinario, req);
        return toResponse(veterinarioRepository.save(veterinario));
    }

    public VeterinarioResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    /** Si activo es null lista todos; si no, filtra por estado. */
    public List<VeterinarioResponse> listar(Boolean activo) {
        List<Veterinario> resultado = (activo == null)
                ? veterinarioRepository.findAll()
                : veterinarioRepository.findByActivo(activo);
        return resultado.stream().map(v -> toResponse(v)).toList();
    }

    private Veterinario buscar(Long id) {
        return veterinarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Veterinario no encontrado (id=" + id + ")"));
    }

    private void aplicar(Veterinario veterinario, VeterinarioRequest req) {
        veterinario.setNombre(req.nombre().trim());
        veterinario.setDocumento(req.documento().trim());
        veterinario.setTelefono(req.telefono().trim());
        veterinario.setEmail(Textos.limpiar(req.email()));
        veterinario.setEspecialidad(Textos.limpiar(req.especialidad()));
        if (req.activo() != null) {
            veterinario.setActivo(req.activo());
        }
    }
}
