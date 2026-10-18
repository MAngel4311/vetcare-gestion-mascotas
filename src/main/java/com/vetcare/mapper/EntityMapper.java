package com.vetcare.mapper;

import com.vetcare.dto.CitaResponse;
import com.vetcare.dto.ConsultaReporteResponse;
import com.vetcare.dto.DuenoResponse;
import com.vetcare.dto.MascotaResponse;
import com.vetcare.dto.RegistroClinicoResponse;
import com.vetcare.dto.VacunacionResponse;
import com.vetcare.dto.VeterinarioResponse;
import com.vetcare.model.Cita;
import com.vetcare.model.Dueno;
import com.vetcare.model.Mascota;
import com.vetcare.model.RegistroClinico;
import com.vetcare.model.Vacunacion;
import com.vetcare.model.Veterinario;

/**
 * Convierte entidades en DTOs de respuesta.
 * Debe llamarse dentro de una transacción (los servicios ya lo hacen) porque
 * algunas relaciones son LAZY.
 */
public final class EntityMapper {

    private EntityMapper() {
    }

    public static DuenoResponse toResponse(Dueno d) {
        return new DuenoResponse(d.getId(), d.getNombre(), d.getDocumento(),
                d.getTelefono(), d.getEmail(), d.getDireccion());
    }

    public static VeterinarioResponse toResponse(Veterinario v) {
        return new VeterinarioResponse(v.getId(), v.getNombre(), v.getDocumento(),
                v.getTelefono(), v.getEmail(), v.getEspecialidad(), v.isActivo());
    }

    public static MascotaResponse toResponse(Mascota m) {
        return new MascotaResponse(m.getId(), m.getNombre(), m.getEspecie(), m.getRaza(),
                m.getFechaNacimiento(), m.getPesoKg(),
                m.getDueno().getId(), m.getDueno().getNombre(), m.isActivo());
    }

    public static CitaResponse toResponse(Cita c) {
        Mascota m = c.getMascota();
        Veterinario v = c.getVeterinario();
        return new CitaResponse(c.getId(), m.getId(), m.getNombre(), m.getDueno().getNombre(),
                v.getId(), v.getNombre(), c.getFechaHoraInicio(), c.getFechaHoraFin(),
                c.getMotivo(), c.getEstado(), c.getMotivoCancelacion());
    }

    public static RegistroClinicoResponse toResponse(RegistroClinico r) {
        return new RegistroClinicoResponse(r.getId(), r.getCita().getId(), r.getCita().getMotivo(),
                r.getMascota().getId(), r.getMascota().getNombre(),
                r.getVeterinario().getId(), r.getVeterinario().getNombre(),
                r.getFechaConsulta(), r.getDiagnostico(), r.getTratamiento(),
                r.getObservaciones(), r.getPesoKg(), r.getTemperaturaC(), r.getAlergias());
    }

    public static VacunacionResponse toResponse(Vacunacion v) {
        Veterinario vet = v.getVeterinario();
        return new VacunacionResponse(v.getId(), v.getMascota().getId(), v.getMascota().getNombre(),
                vet != null ? vet.getId() : null, vet != null ? vet.getNombre() : null,
                v.getNombreVacuna(), v.getFechaAplicacion(), v.getProximaFecha(), v.getObservaciones());
    }

    public static ConsultaReporteResponse toReporte(Cita c) {
        return new ConsultaReporteResponse(c.getId(), c.getFechaHoraInicio(),
                c.getMascota().getNombre(), c.getVeterinario().getNombre(),
                c.getMotivo(), c.getEstado());
    }
}
