package com.vetcare.dto;

public record DuenoResponse(
        Long id,
        String nombre,
        String documento,
        String telefono,
        String email,
        String direccion) {
}
