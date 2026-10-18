package com.vetcare.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Registro propio de un dueño: crea su ficha de dueño y su cuenta con rol USUARIO. */
public record RegistroRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Pattern(regexp = "[A-Za-z0-9._-]{3,50}",
                message = "El usuario debe tener entre 3 y 50 caracteres (letras, números, punto, guion o guion bajo)")
        String username,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotBlank(message = "El documento es obligatorio")
        @Size(max = 20, message = "El documento no puede superar 20 caracteres")
        String documento,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "\\d{10}", message = "El teléfono debe tener 10 dígitos")
        String telefono,

        @Email(message = "El email no tiene un formato válido")
        @Size(max = 120, message = "El email no puede superar 120 caracteres")
        String email,

        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
        String direccion) {
}
