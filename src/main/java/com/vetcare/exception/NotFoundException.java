package com.vetcare.exception;

/** El recurso buscado no existe (HTTP 404). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
