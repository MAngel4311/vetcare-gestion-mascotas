package com.vetcare.repository;

import com.vetcare.model.Cita;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CitaRepository extends JpaRepository<Cita, Long>, JpaSpecificationExecutor<Cita> {

    /**
     * SWR-02: ¿el veterinario ya tiene una cita vigente (PROGRAMADA o CONFIRMADA) que se cruza con [inicio, fin)?
     * Dos rangos se cruzan cuando: inicioExistente < finNuevo Y finExistente > inicioNuevo.
     */
    @Query("""
            SELECT COUNT(c) > 0 FROM Cita c
            WHERE c.veterinario.id = :veterinarioId
              AND c.estado IN (com.vetcare.model.enums.EstadoCita.PROGRAMADA,
                                com.vetcare.model.enums.EstadoCita.CONFIRMADA)
              AND c.fechaHoraInicio < :fin
              AND c.fechaHoraFin > :inicio
            """)
    boolean existeChoque(@Param("veterinarioId") Long veterinarioId,
                         @Param("inicio") LocalDateTime inicio,
                         @Param("fin") LocalDateTime fin);

    /** SWR-11: igual que la anterior, pero ignora la cita que se está reprogramando. */
    @Query("""
            SELECT COUNT(c) > 0 FROM Cita c
            WHERE c.veterinario.id = :veterinarioId
              AND c.id <> :citaId
              AND c.estado IN (com.vetcare.model.enums.EstadoCita.PROGRAMADA,
                                com.vetcare.model.enums.EstadoCita.CONFIRMADA)
              AND c.fechaHoraInicio < :fin
              AND c.fechaHoraFin > :inicio
            """)
    boolean existeChoqueExcluyendo(@Param("veterinarioId") Long veterinarioId,
                                   @Param("citaId") Long citaId,
                                   @Param("inicio") LocalDateTime inicio,
                                   @Param("fin") LocalDateTime fin);

    /** Citas vigentes del veterinario que ocupan algo de tiempo entre desde y hasta, ordenadas por hora. */
    @Query("""
            SELECT c FROM Cita c JOIN FETCH c.mascota
            WHERE c.veterinario.id = :veterinarioId
              AND c.estado IN (com.vetcare.model.enums.EstadoCita.PROGRAMADA,
                                com.vetcare.model.enums.EstadoCita.CONFIRMADA)
              AND c.fechaHoraInicio < :hasta
              AND c.fechaHoraFin > :desde
            ORDER BY c.fechaHoraInicio
            """)
    List<Cita> findProgramadasEnRango(@Param("veterinarioId") Long veterinarioId,
                                      @Param("desde") LocalDateTime desde,
                                      @Param("hasta") LocalDateTime hasta);

    /** RF-14 / RN-07: ¿el veterinario tiene citas vigentes que aún no han ocurrido? */
    boolean existsByVeterinarioIdAndEstadoInAndFechaHoraInicioAfter(
            Long veterinarioId, Collection<com.vetcare.model.enums.EstadoCita> estados, LocalDateTime desde);

    /** RN-08: ¿la mascota tiene citas vigentes que aún no han ocurrido? */
    boolean existsByMascotaIdAndEstadoInAndFechaHoraInicioAfter(
            Long mascotaId, Collection<com.vetcare.model.enums.EstadoCita> estados, LocalDateTime desde);

    /** Citas vigentes que empiezan entre desde y hasta y todavía no tienen recordatorio enviado. */
    @Query("""
            SELECT c FROM Cita c JOIN FETCH c.mascota m JOIN FETCH m.dueno JOIN FETCH c.veterinario
            WHERE c.estado IN (com.vetcare.model.enums.EstadoCita.PROGRAMADA,
                               com.vetcare.model.enums.EstadoCita.CONFIRMADA)
              AND c.recordatorioEnviado = false
              AND c.fechaHoraInicio > :desde AND c.fechaHoraInicio <= :hasta
            ORDER BY c.fechaHoraInicio
            """)
    List<Cita> findParaRecordar(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
