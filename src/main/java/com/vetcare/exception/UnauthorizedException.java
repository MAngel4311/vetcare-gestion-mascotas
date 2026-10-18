package com.vetcare.exception;

/** Credenciales incorrectas al iniciar sesión (HTTP 401). */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
