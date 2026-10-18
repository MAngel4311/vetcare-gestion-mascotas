package com.vetcare.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetcare.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Spring Security responde por su cuenta los 401 y 403; aquí los devolvemos con el mismo formato JSON del resto de la API. */
@Component
@RequiredArgsConstructor
public class RestSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /** No hay token, o es inválido o expiró. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        escribir(response, HttpStatus.UNAUTHORIZED,
                "Debes iniciar sesión (envía el token en el encabezado Authorization: Bearer ...) o tu sesión expiró");
    }

    /** Hay sesión, pero el rol no tiene permiso para esta acción. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        escribir(response, HttpStatus.FORBIDDEN, "Tu rol no tiene permiso para realizar esta acción");
    }

    private void escribir(HttpServletResponse response, HttpStatus estado, String mensaje) throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                new ErrorResponse(LocalDateTime.now(), estado.value(), estado.getReasonPhrase(), mensaje, null));
    }
}
