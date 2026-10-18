package com.vetcare.repository;

import com.vetcare.model.RegistroClinico;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroClinicoRepository extends JpaRepository<RegistroClinico, Long> {

    boolean existsByCitaId(Long citaId);

    /** F-04: historial de una mascota, del más reciente al más antiguo. */
    List<RegistroClinico> findByMascotaIdOrderByFechaConsultaDesc(Long mascotaId);
}
