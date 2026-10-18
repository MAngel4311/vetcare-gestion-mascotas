package com.vetcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Datos comunes de Dueno y Veterinario. */
@MappedSuperclass
@Getter
@Setter
public abstract class Persona extends BaseEntity {

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, unique = true, length = 20)
    private String documento;

    @NotBlank
    @Pattern(regexp = "\\d{10}", message = "El teléfono debe tener 10 dígitos")
    @Column(nullable = false, length = 10)
    private String telefono;

    /**
     * Opcional en la base de datos (SWR-01 no lo pide), pero se necesita
     * para enviar la confirmación de la cita (SWR-04).
     */
    @Email
    @Size(max = 120)
    @Column(length = 120)
    private String email;
}
