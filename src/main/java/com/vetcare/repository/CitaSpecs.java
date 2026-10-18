package com.vetcare.repository;

import com.vetcare.model.Cita;
import com.vetcare.model.enums.EstadoCita;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Filtros opcionales para listar citas y generar el reporte: solo se aplican los que no son null. */
public final class CitaSpecs {

    private CitaSpecs() {
    }

    /** Solo las citas de las mascotas de un dueño (para el rol USUARIO). */
    public static Specification<Cita> delDueno(Long duenoId) {
        return (root, query, cb) -> cb.equal(root.get("mascota").get("dueno").get("id"), duenoId);
    }

    /** desde es inclusive y hasta es exclusivo (sobre la hora de inicio de la cita). */
    public static Specification<Cita> filtrar(LocalDateTime desde, LocalDateTime hasta,
                                              Long veterinarioId, Long mascotaId, EstadoCita estado) {
        return (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();
            if (desde != null) {
                filtros.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("fechaHoraInicio"), desde));
            }
            if (hasta != null) {
                filtros.add(cb.lessThan(root.<LocalDateTime>get("fechaHoraInicio"), hasta));
            }
            if (veterinarioId != null) {
                filtros.add(cb.equal(root.get("veterinario").get("id"), veterinarioId));
            }
            if (mascotaId != null) {
                filtros.add(cb.equal(root.get("mascota").get("id"), mascotaId));
            }
            if (estado != null) {
                filtros.add(cb.equal(root.get("estado"), estado));
            }
            return cb.and(filtros.toArray(new Predicate[0]));
        };
    }
}
