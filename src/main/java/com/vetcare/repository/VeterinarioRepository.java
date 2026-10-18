package com.vetcare.repository;

import com.vetcare.model.Veterinario;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VeterinarioRepository extends JpaRepository<Veterinario, Long> {

    List<Veterinario> findByActivo(boolean activo);

    boolean existsByDocumento(String documento);

    boolean existsByDocumentoAndIdNot(String documento, Long id);

    /**
     * Trae al veterinario bloqueando su fila hasta terminar la transacción.
     * Así, dos recepcionistas que agenden al mismo tiempo con el mismo veterinario
     * no pueden saltarse la validación de choque de horario.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Veterinario v WHERE v.id = :id")
    Optional<Veterinario> findByIdForUpdate(@Param("id") Long id);
}
