package com.vetcare.dto;

public record VeterinarioResponse(
        Long id,
        String nombre,
        String documento,
        String telefono,
        String email,
        String especialidad,
        boolean activo) {
}
