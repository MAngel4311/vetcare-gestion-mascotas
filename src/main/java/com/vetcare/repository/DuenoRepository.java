package com.vetcare.repository;

import com.vetcare.model.Dueno;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DuenoRepository extends JpaRepository<Dueno, Long> {

    Optional<Dueno> findByDocumento(String documento);

    List<Dueno> findByNombreContainingIgnoreCase(String nombre);

    boolean existsByDocumento(String documento);

    boolean existsByDocumentoAndIdNot(String documento, Long id);
}
