package com.vetcare.dto;

/**
 * Respuesta al registrar un dueño en recepción: sus datos más la cuenta que se le creó.
 * La contraseña temporal solo se muestra esta vez; el dueño la cambia con PUT /api/auth/password.
 */
public record DuenoCreadoResponse(
        Long id,
        String nombre,
        String documento,
        String telefono,
        String email,
        String direccion,
        String usuario,
        String passwordTemporal) {
}
