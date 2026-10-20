package com.vetcare.service;

import com.vetcare.repository.DuenoRepository;
import com.vetcare.model.Dueno;
import com.vetcare.dto.UsuarioRequest;
import com.vetcare.dto.UsuarioResponse;
import com.vetcare.dto.UsuarioUpdateRequest;
import com.vetcare.exception.BusinessException;
import com.vetcare.exception.ConflictException;
import com.vetcare.exception.NotFoundException;
import com.vetcare.model.Usuario;
import com.vetcare.model.enums.Rol;
import com.vetcare.repository.UsuarioRepository;
import com.vetcare.repository.VeterinarioRepository;
import com.vetcare.model.Veterinario;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administración de usuarios (solo ADMINISTRADOR). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final DuenoRepository duenoRepository;
    private final VeterinarioRepository veterinarioRepository;

    @Transactional
    public UsuarioResponse crear(UsuarioRequest req) {
        String username = req.username().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByUsername(username)) {
            throw new ConflictException("Ya existe un usuario con el nombre " + username);
        }
        Dueno dueno = null;
        if (req.rol() == Rol.USUARIO) {
            if (req.duenoId() == null) {
                throw new BusinessException("Un usuario con rol USUARIO debe indicar el dueño al que pertenece (duenoId)");
            }
            dueno = duenoRepository.findById(req.duenoId())
                    .orElseThrow(() -> new NotFoundException("Dueño no encontrado (id=" + req.duenoId() + ")"));
            if (usuarioRepository.existsByDuenoId(dueno.getId())) {
                throw new ConflictException("Ese dueño ya tiene una cuenta de usuario");
            }
        }
        Usuario usuario = new Usuario();
        usuario.setDueno(dueno);
        if (req.rol() == Rol.VETERINARIO && req.veterinarioId() != null) {
            usuario.setVeterinario(buscarVeterinario(req.veterinarioId()));
        }
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode(req.password()));
        usuario.setNombreCompleto(req.nombreCompleto().trim());
        usuario.setRol(req.rol());
        usuario.setActivo(true);
        return aRespuesta(usuarioRepository.save(usuario));
    }

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(u -> aRespuesta(u)).toList();
    }

    public UsuarioResponse obtenerPorUsername(String username) {
        return aRespuesta(usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado (" + username + ")")));
    }

    /** usernameActual es quien hace la petición: no puede quitarse el rol de administrador ni desactivarse. */
    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest req, String usernameActual) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado (id=" + id + ")"));

        boolean esUnoMismo = usuario.getUsername().equals(usernameActual);
        boolean desactiva = req.activo() != null && !req.activo();
        if (esUnoMismo && (desactiva || req.rol() != Rol.ADMINISTRADOR)) {
            throw new BusinessException("No puedes quitarte el rol de administrador ni desactivar tu propia cuenta");
        }

        if (req.rol() == Rol.USUARIO && usuario.getDueno() == null) {
            throw new BusinessException("Un usuario con rol USUARIO debe estar vinculado a un dueño");
        }

        if (req.rol() == Rol.VETERINARIO && req.veterinarioId() != null) {
            usuario.setVeterinario(buscarVeterinario(req.veterinarioId()));
        }
        usuario.setNombreCompleto(req.nombreCompleto().trim());
        usuario.setRol(req.rol());
        if (req.activo() != null) {
            usuario.setActivo(req.activo());
        }
        if (req.password() != null && !req.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        return aRespuesta(usuarioRepository.save(usuario));
    }

    private UsuarioResponse aRespuesta(Usuario u) {
        Long duenoId = u.getDueno() != null ? u.getDueno().getId() : null;
        Long vetId = u.getVeterinario() != null ? u.getVeterinario().getId() : null;
        return new UsuarioResponse(u.getId(), u.getUsername(), u.getNombreCompleto(), u.getRol(), u.isActivo(), duenoId, vetId);
    }

    private Veterinario buscarVeterinario(Long id) {
        return veterinarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Veterinario no encontrado (id=" + id + ")"));
    }
}
