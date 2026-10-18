package com.vetcare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** RF-09: toda cancelación debe indicar su motivo. */
public record CancelarCitaRequest(
        @NotBlank(message = "El motivo de cancelación es obligatorio")
        @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
        String motivo) {
}
