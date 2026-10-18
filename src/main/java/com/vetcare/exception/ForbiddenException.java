package com.vetcare.exception;

/** El usuario tiene sesión pero no puede acceder a ese recurso (HTTP 403). */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
