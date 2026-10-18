package com.vetcare.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * F-03 / SWR-06 y SWR-08. La mascota, el veterinario y la fecha NO se envían:
 * el servidor los toma de la cita (SWR-07).
 */
public record RegistroClinicoRequest(
        @NotBlank(message = "El diagnóstico es obligatorio")
        @Size(max = 2000, message = "El diagnóstico no puede superar 2000 caracteres")
        String diagnostico,

        @NotBlank(message = "El tratamiento es obligatorio")
        @Size(max = 2000, message = "El tratamiento no puede superar 2000 caracteres")
        String tratamiento,

        @Size(max = 2000, message = "Las observaciones no pueden superar 2000 caracteres")
        String observaciones,

        @Positive(message = "El peso debe ser mayor que 0")
        @Digits(integer = 4, fraction = 2, message = "El peso admite hasta 4 enteros y 2 decimales")
        BigDecimal pesoKg,

        @DecimalMin(value = "30.0", message = "La temperatura debe estar entre 30 y 45 °C")
        @DecimalMax(value = "45.0", message = "La temperatura debe estar entre 30 y 45 °C")
        @Digits(integer = 3, fraction = 1, message = "La temperatura admite 1 decimal")
        BigDecimal temperaturaC,

        @Size(max = 500, message = "Las alergias no pueden superar 500 caracteres")
        String alergias) {
}
