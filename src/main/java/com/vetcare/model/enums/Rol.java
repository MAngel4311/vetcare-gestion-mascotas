package com.vetcare.model.enums;

/** Actores que mencionan los requisitos de stakeholder. El ADMINISTRADOR puede hacer todo. */
public enum Rol {
    RECEPCIONISTA,
    VETERINARIO,
    ADMINISTRADOR,
    /** Dueño de mascotas: se registra solo y gestiona únicamente lo suyo. */
    USUARIO
}
