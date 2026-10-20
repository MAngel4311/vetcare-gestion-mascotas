package com.vetcare.dto;

/**
 * Respuesta al crear un veterinario: sus datos más la cuenta VETERINARIO que se le creó.
 * La contraseña temporal solo se muestra esta vez; el veterinario la cambia con PUT /api/auth/password.
 */
public record VeterinarioCreadoResponse(
        Long id,
        String nombre,
        String documento,
        String telefono,
        String email,
        String especialidad,
        boolean activo,
        String usuario,
        String passwordTemporal) {
}
