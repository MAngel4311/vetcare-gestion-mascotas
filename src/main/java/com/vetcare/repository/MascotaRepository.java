package com.vetcare.repository;

import com.vetcare.model.Mascota;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MascotaRepository extends JpaRepository<Mascota, Long> {

    List<Mascota> findByDuenoId(Long duenoId);

    List<Mascota> findByNombreContainingIgnoreCase(String nombre);

    List<Mascota> findByDuenoIdAndNombreContainingIgnoreCase(Long duenoId, String nombre);

    /** Evita dos mascotas con el mismo nombre para un mismo dueño (sin importar mayúsculas). */
    boolean existsByDuenoIdAndNombreIgnoreCase(Long duenoId, String nombre);

    boolean existsByDuenoIdAndNombreIgnoreCaseAndIdNot(Long duenoId, String nombre, Long id);
}
