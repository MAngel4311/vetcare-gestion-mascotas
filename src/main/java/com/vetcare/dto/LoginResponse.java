package com.vetcare.dto;

import com.vetcare.model.enums.Rol;

/** El frontend debe enviar el token en cada petición: Authorization: Bearer {token}. */
public record LoginResponse(
        String token,
        String tipo,
        long expiraEnSegundos,
        String username,
        String nombreCompleto,
        Rol rol,
        Long duenoId) {
}
