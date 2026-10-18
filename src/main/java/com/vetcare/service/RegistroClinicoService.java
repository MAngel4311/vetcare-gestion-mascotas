package com.vetcare.service;

import com.vetcare.model.Mascota;
import com.vetcare.security.UsuarioActual;
import static com.vetcare.mapper.EntityMapper.toResponse;

import com.vetcare.dto.RegistroClinicoRequest;
import com.vetcare.dto.RegistroClinicoResponse;
import com.vetcare.exception.ConflictException;
import com.vetcare.exception.NotFoundException;
import com.vetcare.model.Cita;
import com.vetcare.model.RegistroClinico;
import com.vetcare.model.enums.EstadoCita;
import com.vetcare.repository.CitaRepository;
import com.vetcare.repository.MascotaRepository;
import com.vetcare.repository.RegistroClinicoRepository;
import com.vetcare.util.Textos;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F-03 (registrar consulta) y F-04 (consultar historial). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegistroClinicoService {

    private final RegistroClinicoRepository registroRepository;
    private final CitaRepository citaRepository;
    private final MascotaRepository mascotaRepository;
    private final UsuarioActual usuarioActual;

    /**
     * SWR-06, SWR-07 y SWR-08. La mascota, el veterinario y la fecha se toman de la cita,
     * y la cita pasa a ATENDIDA.
     */
    @Transactional
    public RegistroClinicoResponse registrar(Long citaId, RegistroClinicoRequest req) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada (id=" + citaId + ")"));
        if (registroRepository.existsByCitaId(citaId)) {
            throw new ConflictException("La cita ya tiene un registro clínico");
        }
        // RN-04: la consulta solo se registra sobre una cita CONFIRMADA
        if (cita.getEstado() != EstadoCita.CONFIRMADA) {
            throw new ConflictException(
                    "Solo se registra la consulta de una cita CONFIRMADA (estado actual: " + cita.getEstado() + ")");
        }

        RegistroClinico registro = new RegistroClinico();
        registro.setCita(cita);
        registro.setMascota(cita.getMascota());
        registro.setVeterinario(cita.getVeterinario());
        registro.setFechaConsulta(cita.getFechaHoraInicio());
        registro.setDiagnostico(req.diagnostico().trim());
        registro.setTratamiento(req.tratamiento().trim());
        registro.setObservaciones(Textos.limpiar(req.observaciones()));
        registro.setPesoKg(req.pesoKg());
        registro.setTemperaturaC(req.temperaturaC());
        registro.setAlergias(Textos.limpiar(req.alergias()));

        cita.setEstado(EstadoCita.ATENDIDA);
        citaRepository.save(cita);
        return toResponse(registroRepository.save(registro));
    }

    /** SWR-09: historial de una mascota, del más reciente al más antiguo. */
    public List<RegistroClinicoResponse> historial(Long mascotaId) {
        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new NotFoundException("Mascota no encontrada (id=" + mascotaId + ")"));
        usuarioActual.exigirMascotaPropia(mascota);
        return registroRepository.findByMascotaIdOrderByFechaConsultaDesc(mascotaId)
                .stream()
                .map(r -> toResponse(r))
                .toList();
    }
}
