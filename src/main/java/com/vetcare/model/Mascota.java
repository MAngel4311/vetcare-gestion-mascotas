package com.vetcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** F-05 / SWR-10: mascota asociada a un dueño ya registrado. */
@Entity
@Table(name = "mascotas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mascota_dueno_nombre",
                columnNames = {"dueno_id", "nombre"}))
@Getter
@Setter
public class Mascota extends BaseEntity {

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String especie;

    @Size(max = 50)
    @Column(length = 50)
    private String raza;

    @NotNull
    @PastOrPresent
    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal pesoKg;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dueno_id", nullable = false)
    private Dueno dueno;

    /** Baja lógica: una mascota inactiva conserva su historial pero no admite citas nuevas. */
    @Column(nullable = false, columnDefinition = "boolean not null default true")
    private boolean activo = true;
}
