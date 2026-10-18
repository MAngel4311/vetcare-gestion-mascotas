package com.vetcare.dto;

/** Resultado de enviar un correo de prueba: enviado=true, o el motivo si no se pudo. */
public record PruebaCorreoResponse(boolean enviado, boolean correoConfigurado, String detalle) {
}
