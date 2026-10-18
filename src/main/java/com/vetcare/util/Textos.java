package com.vetcare.util;

public final class Textos {

    private Textos() {
    }

    /** Quita espacios al inicio y al final; si queda vacío devuelve null. */
    public static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
