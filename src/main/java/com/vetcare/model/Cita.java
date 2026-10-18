package com.vetcare.model;

import com.vetcare.model.enums.EstadoCita;
import com.vetcare.model.enums.MotivoCita;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * F-02 y F-06: cita veterinaria.
 * Se guarda inicio y fin como fecha-hora para poder detectar cruces de horario.
 */
@Entity
@Table(name = "citas",
        indexes = @Index(name = "idx_citas_veterinario_inicio",
                columnList = "veterinario_id, fecha_hora_inicio"))
@Getter
@Setter
public class Cita extends BaseEntity {

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
    private LocalDateTime fechaHoraInicio;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime fechaHoraFin;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MotivoCita motivo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCita estado = EstadoCita.PROGRAMADA;

    /** true cuando ya se envió el recordatorio previo a la cita. */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean recordatorioEnviado = false;

    /** Se llena al cancelar (obligatorio en ese momento). */
    @Size(max = 255)
    @Column(length = 255)
    private String motivoCancelacion;
}
