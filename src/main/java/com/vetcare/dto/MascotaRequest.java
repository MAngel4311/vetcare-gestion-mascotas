package com.vetcare.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** F-05 / SWR-10. El dueño debe existir (se selecciona por id). */
public record MascotaRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotBlank(message = "La especie es obligatoria")
        @Size(max = 50, message = "La especie no puede superar 50 caracteres")
        String especie,

        @Size(max = 50, message = "La raza no puede superar 50 caracteres")
        String raza,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
        LocalDate fechaNacimiento,

        @NotNull(message = "El peso es obligatorio")
        @Positive(message = "El peso debe ser mayor que 0")
        @Digits(integer = 4, fraction = 2, message = "El peso admite hasta 4 enteros y 2 decimales")
        BigDecimal pesoKg,

        @NotNull(message = "El dueño es obligatorio")
        Long duenoId,

        /** Si no se envía, la mascota queda activa. false = dar de baja. */
        Boolean activo) {
}
