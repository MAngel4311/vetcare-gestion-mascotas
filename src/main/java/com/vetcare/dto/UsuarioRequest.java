package com.vetcare.dto;

import com.vetcare.model.enums.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Pattern(regexp = "[A-Za-z0-9._-]{3,50}",
                message = "El usuario debe tener entre 3 y 50 caracteres (letras, números, punto, guion o guion bajo)")
        String username,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombreCompleto,

        @NotNull(message = "El rol es obligatorio")
        Rol rol,

        /** Obligatorio solo cuando el rol es USUARIO: dueño al que pertenece la cuenta. */
        Long duenoId) {
}
