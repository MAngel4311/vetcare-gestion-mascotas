package com.vetcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** F-08 / SWR-13: vacuna aplicada a una mascota. */
@Entity
@Table(name = "vacunaciones")
@Getter
@Setter
public class Vacunacion extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mascota_id", nullable = false)
    private Mascota mascota;

    /** Opcional: quién aplicó la vacuna. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinario_id")
    private Veterinario veterinario;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombreVacuna;

    @NotNull
    @Column(nullable = false)
    private LocalDate fechaAplicacion;

    @Column
    private LocalDate proximaFecha;

    @Size(max = 500)
    @Column(length = 500)
    private String observaciones;

    /** true cuando ya se envió el correo de recordatorio de la próxima dosis. */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean recordatorioEnviado = false;

    @AssertTrue(message = "La próxima fecha debe ser posterior a la fecha de aplicación")
    private boolean isFechasValidas() {
        return proximaFecha == null
                || fechaAplicacion == null
                || proximaFecha.isAfter(fechaAplicacion);
    }
}
