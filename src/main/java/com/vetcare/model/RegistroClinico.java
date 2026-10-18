package com.vetcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * F-03 / F-04: registro clínico de una consulta.
 * mascota, veterinario y fechaConsulta se copian de la cita en el servicio (SWR-07).
 */
@Entity
@Table(name = "registros_clinicos")
@Getter
@Setter
public class RegistroClinico extends BaseEntity {

    /** Una cita tiene como máximo un registro clínico. */
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cita_id", nullable = false, unique = true)
    private Cita cita;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mascota_id", nullable = false)
    private Mascota mascota;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veterinario_id", nullable = false)
    private Veterinario veterinario;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime fechaConsulta;

    @NotBlank
    @Size(max = 2000)
    @Column(nullable = false, length = 2000)
    private String diagnostico;

    @NotBlank
    @Size(max = 2000)
    @Column(nullable = false, length = 2000)
    private String tratamiento;

    @Size(max = 2000)
    @Column(length = 2000)
    private String observaciones;

    // SWR-08: observaciones del examen físico
    @Positive
    @Column(precision = 6, scale = 2)
    private BigDecimal pesoKg;

    @Column(precision = 4, scale = 1)
    private BigDecimal temperaturaC;

    @Size(max = 500)
    @Column(length = 500)
    private String alergias;
}
