package com.vetcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Veterinario que atiende citas y registra consultas (F-02, F-03). */
@Entity
@Table(name = "veterinarios")
@Getter
@Setter
public class Veterinario extends Persona {

    @Size(max = 100)
    @Column(length = 100)
    private String especialidad;

    /** En vez de borrar un veterinario con historial, se desactiva. */
    @Column(nullable = false)
    private boolean activo = true;
}
