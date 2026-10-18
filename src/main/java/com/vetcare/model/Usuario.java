package com.vetcare.model;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import com.vetcare.model.enums.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Usuario que inicia sesión en el sistema. La contraseña se guarda siempre con hash BCrypt. */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class Usuario extends BaseEntity {

    /** Siempre en minúsculas. */
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String passwordHash;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombreCompleto;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    /** Solo para el rol USUARIO: el dueño cuyas mascotas puede ver y gestionar. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dueno_id")
    private Dueno dueno;
}
