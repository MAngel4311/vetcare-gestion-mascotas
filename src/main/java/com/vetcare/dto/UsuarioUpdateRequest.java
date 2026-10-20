package com.vetcare.dto;

import com.vetcare.model.enums.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** El username no se puede cambiar. Omite "password" (o envíalo null) para no cambiar la contraseña. */
public record UsuarioUpdateRequest(
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombreCompleto,

        @NotNull(message = "El rol es obligatorio")
        Rol rol,

        Boolean activo,

        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,
        /** Opcional: vincula la cuenta a un veterinario (rol VETERINARIO). */
        Long veterinarioId) {
}
