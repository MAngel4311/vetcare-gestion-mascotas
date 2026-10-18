package com.vetcare.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** F-01 / SWR-01. El email es opcional pero necesario para recibir la confirmación de citas (SWR-04). */
public record DuenoRequest(
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
