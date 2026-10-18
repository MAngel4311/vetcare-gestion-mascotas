package com.vetcare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** F-08 / SWR-13. La mascota va en la URL; el veterinario es opcional. */
public record VacunacionRequest(
        @NotBlank(message = "El nombre de la vacuna es obligatorio")
        @Size(max = 100, message = "El nombre de la vacuna no puede superar 100 caracteres")
        String nombreVacuna,

        @NotNull(message = "La fecha de aplicación es obligatoria")
        @PastOrPresent(message = "La fecha de aplicación no puede ser futura")
        LocalDate fechaAplicacion,

        LocalDate proximaFecha,

        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
        String observaciones,

        Long veterinarioId) {
}
