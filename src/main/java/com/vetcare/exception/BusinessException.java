package com.vetcare.exception;

/** Se incumple una regla de negocio con datos inválidos (HTTP 400): fechas en el pasado, rangos al revés, etc. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
