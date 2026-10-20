package com.vetcare.service;

import com.vetcare.util.Textos;
import com.vetcare.repository.DuenoRepository;
import com.vetcare.model.enums.Rol;
import com.vetcare.model.Dueno;
import com.vetcare.exception.ConflictException;
import com.vetcare.dto.RegistroRequest;
import com.vetcare.dto.CambiarPasswordRequest;
import com.vetcare.dto.LoginRequest;
import com.vetcare.dto.LoginResponse;
import com.vetcare.exception.UnauthorizedException;
import com.vetcare.model.Usuario;
import com.vetcare.repository.UsuarioRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Inicio de sesión: valida usuario y contraseña y entrega un token JWT firmado. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final String MSG_CREDENCIALES = "Usuario o contraseña incorrectos";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final DuenoRepository duenoRepository;
    private final com.vetcare.repository.VeterinarioRepository veterinarioRepository;

    @Value("${vetcare.jwt.expiracion-minutos:480}")
    private long expiracionMinutos;

    @Transactional
    public LoginResponse login(LoginRequest req) {
        String username = req.username().trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException(MSG_CREDENCIALES));

        // Mismo mensaje para usuario inexistente, inactivo o contraseña errónea (no revela cuál falló)
        if (!usuario.isActivo() || !passwordEncoder.matches(req.password(), usuario.getPasswordHash())) {
            throw new UnauthorizedException(MSG_CREDENCIALES);
        }

        return emitirToken(usuario);
    }

    /** Registro propio de un dueño: crea el dueño y su cuenta USUARIO, y abre la sesión de una vez. */
    @Transactional
    public LoginResponse registrar(RegistroRequest req) {
        String username = req.username().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByUsername(username)) {
            throw new ConflictException("Ya existe un usuario con el nombre " + username);
        }
        String documento = req.documento().trim();
        if (duenoRepository.existsByDocumento(documento)) {
            throw new ConflictException("Ya existe un dueño con el documento " + documento
                    + ". Si ya eres cliente de la clínica, pide a recepción que te cree la cuenta");
        }

        Dueno dueno = new Dueno();
        dueno.setNombre(req.nombre().trim());
        dueno.setDocumento(documento);
        dueno.setTelefono(req.telefono().trim());
        dueno.setEmail(Textos.limpiar(req.email()));
        dueno.setDireccion(req.direccion().trim());
        dueno = duenoRepository.save(dueno);

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode(req.password()));
        usuario.setNombreCompleto(dueno.getNombre());
        usuario.setRol(Rol.USUARIO);
        usuario.setActivo(true);
        usuario.setDueno(dueno);
        usuarioRepository.save(usuario);
        return emitirToken(usuario);
    }

    /** El usuario con sesión abierta cambia su propia contraseña (el dueño lo hace tras recibir la temporal). */
    @Transactional
    public void cambiarPassword(String username, CambiarPasswordRequest req) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException(MSG_CREDENCIALES));
        if (!passwordEncoder.matches(req.actual(), usuario.getPasswordHash())) {
            throw new UnauthorizedException("La contraseña actual no es correcta");
        }
        usuario.setPasswordHash(passwordEncoder.encode(req.nueva()));
        usuarioRepository.save(usuario);
    }

    private LoginResponse emitirToken(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(expiracionMinutos, ChronoUnit.MINUTES);
        Long duenoId = usuario.getDueno() != null ? usuario.getDueno().getId() : null;
        Long veterinarioId = usuario.getVeterinario() != null ? usuario.getVeterinario().getId() : null;
        if (veterinarioId == null && usuario.getRol() == com.vetcare.model.enums.Rol.VETERINARIO) {
            // Cuentas antiguas: el usuario es el documento del veterinario, se vinculan solas al iniciar sesión
            var vinculado = veterinarioRepository.findByDocumento(usuario.getUsername());
            if (vinculado.isPresent()) {
                usuario.setVeterinario(vinculado.get());
                usuarioRepository.save(usuario);
                veterinarioId = vinculado.get().getId();
            }
        }
        JwtClaimsSet.Builder constructor = JwtClaimsSet.builder()
                .issuer("vet-care")
                .issuedAt(ahora)
                .expiresAt(expira)
                .subject(usuario.getUsername())
                .claim("roles", List.of(usuario.getRol().name()));
        if (duenoId != null) {
            constructor.claim("duenoId", duenoId);
        }
        if (veterinarioId != null) {
            constructor.claim("veterinarioId", veterinarioId);
        }
        JwsHeader encabezado = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(encabezado, constructor.build())).getTokenValue();

        return new LoginResponse(token, "Bearer", expiracionMinutos * 60,
                usuario.getUsername(), usuario.getNombreCompleto(), usuario.getRol(), duenoId, veterinarioId);
    }
}
