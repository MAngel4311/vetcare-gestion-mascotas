package com.vetcare.controller;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import com.vetcare.dto.RegistroRequest;
import com.vetcare.dto.CambiarPasswordRequest;
import com.vetcare.dto.LoginRequest;
import org.springframework.web.bind.annotation.PutMapping;
import com.vetcare.dto.LoginResponse;
import com.vetcare.dto.UsuarioResponse;
import com.vetcare.service.AuthService;
import com.vetcare.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    /** Pública. Devuelve el token que se debe enviar como "Authorization: Bearer {token}". */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    /** Pública. Un dueño se registra solo: se crea su ficha y su cuenta USUARIO, y se devuelve el token. */
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse registro(@Valid @RequestBody RegistroRequest req) {
        return authService.registrar(req);
    }

    /** Cambia la contraseña del usuario con la sesión abierta. */
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CambiarPasswordRequest req) {
        authService.cambiarPassword(jwt.getSubject(), req);
    }

    /** Datos del usuario que tiene la sesión abierta (útil para que el frontend sepa qué rol mostrar). */
    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
        return usuarioService.obtenerPorUsername(jwt.getSubject());
    }
}
