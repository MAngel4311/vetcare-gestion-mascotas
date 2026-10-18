package com.vetcare.exception;

/** La operación choca con el estado actual de los datos (HTTP 409): duplicados, choque de horario, estado inválido. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
