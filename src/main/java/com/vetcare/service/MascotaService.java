package com.vetcare.service;

import com.vetcare.exception.ForbiddenException;
import com.vetcare.security.UsuarioActual;
import static com.vetcare.mapper.EntityMapper.toResponse;

import com.vetcare.dto.MascotaRequest;
import com.vetcare.dto.MascotaResponse;
import com.vetcare.exception.ConflictException;
import com.vetcare.exception.NotFoundException;
import com.vetcare.model.Dueno;
import com.vetcare.model.Mascota;
import com.vetcare.model.enums.EstadoCita;
import com.vetcare.repository.CitaRepository;
import com.vetcare.repository.DuenoRepository;
import com.vetcare.repository.MascotaRepository;
import com.vetcare.util.Textos;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F-05: registrar y buscar mascotas. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MascotaService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final MascotaRepository mascotaRepository;
    private final CitaRepository citaRepository;
    private final DuenoRepository duenoRepository;
    private final UsuarioActual usuarioActual;

    @Transactional
    public MascotaResponse crear(MascotaRequest req) {
        exigirSoloPropio(req.duenoId());
        Dueno dueno = buscarDueno(req.duenoId());
        String nombre = req.nombre().trim();
        if (mascotaRepository.existsByDuenoIdAndNombreIgnoreCase(dueno.getId(), nombre)) {
            throw new ConflictException("El dueño ya tiene una mascota llamada " + nombre);
        }
        Mascota mascota = new Mascota();
        aplicar(mascota, req, dueno);
        return toResponse(mascotaRepository.save(mascota));
    }

    @Transactional
    public MascotaResponse actualizar(Long id, MascotaRequest req) {
        Mascota mascota = buscar(id);
        usuarioActual.exigirMascotaPropia(mascota);
        exigirSoloPropio(req.duenoId());
        Dueno dueno = buscarDueno(req.duenoId());
        String nombre = req.nombre().trim();
        if (mascotaRepository.existsByDuenoIdAndNombreIgnoreCaseAndIdNot(dueno.getId(), nombre, id)) {
            throw new ConflictException("El dueño ya tiene otra mascota llamada " + nombre);
        }
        // RN-08: no se da de baja una mascota con citas futuras vigentes
        if (Boolean.FALSE.equals(req.activo()) && mascota.isActivo()
                && citaRepository.existsByMascotaIdAndEstadoInAndFechaHoraInicioAfter(
                        id, EstadoCita.VIGENTES, LocalDateTime.now(ZONA))) {
            throw new ConflictException("No se puede dar de baja: la mascota tiene citas futuras "
                    + "programadas o confirmadas. Cancélalas primero");
        }
        aplicar(mascota, req, dueno);
        return toResponse(mascotaRepository.save(mascota));
    }

    public MascotaResponse obtener(Long id) {
        Mascota mascota = buscar(id);
        usuarioActual.exigirMascotaPropia(mascota);
        return toResponse(mascota);
    }

    /** Búsqueda por nombre (parcial), dueño y/o especie (RF-04); sin filtros lista todas. */
    public List<MascotaResponse> buscar(String nombre, Long duenoIdPedido, String especie) {
        // Un USUARIO solo ve las mascotas de su propio dueño, sin importar el filtro que envíe
        Long duenoId = usuarioActual.duenoRestringido().orElse(duenoIdPedido);
        String nom = Textos.limpiar(nombre);
        List<Mascota> resultado;
        if (nom != null && duenoId != null) {
            resultado = mascotaRepository.findByDuenoIdAndNombreContainingIgnoreCase(duenoId, nom);
        } else if (nom != null) {
            resultado = mascotaRepository.findByNombreContainingIgnoreCase(nom);
        } else if (duenoId != null) {
            resultado = mascotaRepository.findByDuenoId(duenoId);
        } else {
            resultado = mascotaRepository.findAll();
        }
        String esp = Textos.limpiar(especie);
        return resultado.stream()
                .filter(m -> esp == null || m.getEspecie().equalsIgnoreCase(esp))
                .map(m -> toResponse(m))
                .toList();
    }

    private Mascota buscar(Long id) {
        return mascotaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mascota no encontrada (id=" + id + ")"));
    }

    /** Un USUARIO solo puede registrar o dejar mascotas a su propio nombre. */
    private void exigirSoloPropio(Long duenoId) {
        usuarioActual.duenoRestringido().ifPresent(propio -> {
            if (!propio.equals(duenoId)) {
                throw new ForbiddenException("Solo puedes gestionar mascotas a tu nombre");
            }
        });
    }

    private Dueno buscarDueno(Long id) {
        return duenoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Dueño no encontrado (id=" + id + ")"));
    }

    private void aplicar(Mascota mascota, MascotaRequest req, Dueno dueno) {
        mascota.setNombre(req.nombre().trim());
        mascota.setEspecie(req.especie().trim());
        mascota.setRaza(Textos.limpiar(req.raza()));
        mascota.setFechaNacimiento(req.fechaNacimiento());
        mascota.setPesoKg(req.pesoKg());
        mascota.setDueno(dueno);
        if (req.activo() != null) {
            mascota.setActivo(req.activo());
        }
    }
}
