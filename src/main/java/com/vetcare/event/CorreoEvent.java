package com.vetcare.event;

/** Un correo listo para enviar; se manda solo cuando la transacción que lo publicó termina bien. */
public record CorreoEvent(String para, String asunto, String texto) {
}
