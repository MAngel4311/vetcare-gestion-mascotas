package com.vetcare.dto;

import com.vetcare.model.enums.Rol;

public record UsuarioResponse(
        Long id,
        String username,
        String nombreCompleto,
        Rol rol,
        boolean activo,
        Long duenoId) {
}
