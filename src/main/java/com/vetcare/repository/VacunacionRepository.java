package com.vetcare.repository;

import com.vetcare.model.Vacunacion;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VacunacionRepository extends JpaRepository<Vacunacion, Long> {

    List<Vacunacion> findByMascotaIdOrderByFechaAplicacionDesc(Long mascotaId);

    /**
     * Vacunas con próxima dosis entre desde y hasta que todavía NO se han vuelto a aplicar:
     * si la misma mascota ya recibió después la misma vacuna, ese seguimiento se considera cumplido.
     */
    @Query("""
            SELECT v FROM Vacunacion v
            JOIN FETCH v.mascota m
            JOIN FETCH m.dueno
            WHERE v.proximaFecha IS NOT NULL
              AND v.proximaFecha BETWEEN :desde AND :hasta
              AND NOT EXISTS (
                  SELECT 1 FROM Vacunacion v2
                  WHERE v2.mascota = v.mascota
                    AND LOWER(v2.nombreVacuna) = LOWER(v.nombreVacuna)
                    AND v2.fechaAplicacion > v.fechaAplicacion)
            ORDER BY v.proximaFecha
            """)
    List<Vacunacion> findPendientes(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** RF-13: igual que findPendientes, pero solo las que todavía no tienen recordatorio enviado. */
    @Query("""
            SELECT v FROM Vacunacion v
            JOIN FETCH v.mascota m
            JOIN FETCH m.dueno
            WHERE v.proximaFecha IS NOT NULL
              AND v.proximaFecha BETWEEN :desde AND :hasta
              AND v.recordatorioEnviado = false
              AND m.activo = true
              AND NOT EXISTS (
                  SELECT 1 FROM Vacunacion v2
                  WHERE v2.mascota = v.mascota
                    AND LOWER(v2.nombreVacuna) = LOWER(v.nombreVacuna)
                    AND v2.fechaAplicacion > v.fechaAplicacion)
            ORDER BY v.proximaFecha
            """)
    List<Vacunacion> findParaRecordar(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
