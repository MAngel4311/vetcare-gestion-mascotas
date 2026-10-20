package com.vetcare.service;

import static com.vetcare.mapper.EntityMapper.toResponse;

import com.vetcare.dto.VeterinarioCreadoResponse;
import com.vetcare.dto.VeterinarioRequest;
import com.vetcare.event.CorreoEvent;
import com.vetcare.model.Usuario;
import com.vetcare.model.enums.Rol;
import com.vetcare.repository.UsuarioRepository;
import java.security.SecureRandom;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.vetcare.dto.VeterinarioResponse;
import com.vetcare.exception.ConflictException;
import com.vetcare.exception.NotFoundException;
import com.vetcare.model.Veterinario;
import com.vetcare.model.enums.EstadoCita;
import com.vetcare.repository.CitaRepository;
import com.vetcare.repository.VeterinarioRepository;
import com.vetcare.util.Textos;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestión básica de veterinarios (se necesitan para citas, historial y reporte). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VeterinarioService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final VeterinarioRepository veterinarioRepository;
    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    private static final String ALFABETO = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom AZAR = new SecureRandom();

    /**
     * El administrador crea al veterinario y, en el mismo paso, su cuenta VETERINARIO:
     * usuario = su documento y una contraseña temporal que se devuelve una sola vez.
     * Si tiene correo, también se le envía por correo.
     */
    @Transactional
    public VeterinarioCreadoResponse crear(VeterinarioRequest req) {
        String documento = req.documento().trim();
        if (veterinarioRepository.existsByDocumento(documento)) {
            throw new ConflictException("Ya existe un veterinario con el documento " + documento);
        }
        String username = documento.toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByUsername(username)) {
            throw new ConflictException("Ya existe un usuario con el nombre " + username);
        }
        Veterinario veterinario = new Veterinario();
        aplicar(veterinario, req);
        veterinario = veterinarioRepository.save(veterinario);

        StringBuilder clave = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            clave.append(ALFABETO.charAt(AZAR.nextInt(ALFABETO.length())));
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode(clave));
        usuario.setNombreCompleto(veterinario.getNombre());
        usuario.setRol(Rol.VETERINARIO);
        usuario.setVeterinario(veterinario);
        usuario.setActivo(true);
        usuarioRepository.save(usuario);

        eventPublisher.publishEvent(new CorreoEvent(veterinario.getEmail(), "Tu cuenta en Vet-Care",
                "Hola " + veterinario.getNombre() + ",\n\nTe registramos como veterinario en Vet-Care y creamos tu "
                        + "cuenta para que consultes tu agenda y el historial de tus pacientes:\n\n  Usuario: "
                        + username + "\n  Clave temporal: " + clave
                        + "\n\nCámbiala la primera vez que entres.\n\nVet-Care"));

        return new VeterinarioCreadoResponse(veterinario.getId(), veterinario.getNombre(),
                veterinario.getDocumento(), veterinario.getTelefono(), veterinario.getEmail(),
                veterinario.getEspecialidad(), veterinario.isActivo(), username, clave.toString());
    }

    @Transactional
    public VeterinarioResponse actualizar(Long id, VeterinarioRequest req) {
        Veterinario veterinario = buscar(id);
        String documento = req.documento().trim();
        if (veterinarioRepository.existsByDocumentoAndIdNot(documento, id)) {
            throw new ConflictException("Ya existe otro veterinario con el documento " + documento);
        }
        // RF-14 / RN-07: no se desactiva un veterinario que aún tiene citas por atender
        if (Boolean.FALSE.equals(req.activo()) && veterinario.isActivo()
                && citaRepository.existsByVeterinarioIdAndEstadoInAndFechaHoraInicioAfter(
                        id, EstadoCita.VIGENTES, LocalDateTime.now(ZONA))) {
            throw new ConflictException("No se puede desactivar: el veterinario tiene citas futuras "
                    + "programadas o confirmadas. Cancélalas o reprogramalas primero");
        }
        aplicar(veterinario, req);
        return toResponse(veterinarioRepository.save(veterinario));
    }

    public VeterinarioResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    /** Si activo es null lista todos; si no, filtra por estado. */
    public List<VeterinarioResponse> listar(Boolean activo) {
        List<Veterinario> resultado = (activo == null)
                ? veterinarioRepository.findAll()
                : veterinarioRepository.findByActivo(activo);
        return resultado.stream().map(v -> toResponse(v)).toList();
    }

    private Veterinario buscar(Long id) {
        return veterinarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Veterinario no encontrado (id=" + id + ")"));
    }

    private void aplicar(Veterinario veterinario, VeterinarioRequest req) {
        veterinario.setNombre(req.nombre().trim());
        veterinario.setDocumento(req.documento().trim());
        veterinario.setTelefono(req.telefono().trim());
        veterinario.setEmail(Textos.limpiar(req.email()));
        veterinario.setEspecialidad(Textos.limpiar(req.especialidad()));
        if (req.activo() != null) {
            veterinario.setActivo(req.activo());
        }
    }
}
