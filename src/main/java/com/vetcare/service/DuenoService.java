package com.vetcare.service;

import com.vetcare.exception.ForbiddenException;
import com.vetcare.security.UsuarioActual;
import static com.vetcare.mapper.EntityMapper.toResponse;

import com.vetcare.dto.DuenoRequest;
import com.vetcare.dto.DuenoResponse;
import com.vetcare.exception.ConflictException;
import com.vetcare.exception.NotFoundException;
import com.vetcare.model.Dueno;
import com.vetcare.repository.DuenoRepository;
import com.vetcare.util.Textos;
import com.vetcare.dto.DuenoCreadoResponse;
import com.vetcare.model.Usuario;
import com.vetcare.model.enums.Rol;
import com.vetcare.repository.UsuarioRepository;
import com.vetcare.event.CorreoEvent;
import java.security.SecureRandom;
import org.springframework.context.ApplicationEventPublisher;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F-01: registrar y consultar dueños. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DuenoService {

    private final DuenoRepository duenoRepository;
    private final UsuarioActual usuarioActual;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    private static final String ALFABETO = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom AZAR = new SecureRandom();

    /**
     * Recepción registra al dueño y, en el mismo paso, se le crea su cuenta USUARIO:
     * usuario = su documento y una contraseña temporal que se devuelve una sola vez.
     */
    @Transactional
    public DuenoCreadoResponse crear(DuenoRequest req) {
        String documento = req.documento().trim();
        if (duenoRepository.existsByDocumento(documento)) {
            throw new ConflictException("Ya existe un dueño con el documento " + documento);
        }
        String username = documento.toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByUsername(username)) {
            throw new ConflictException("Ya existe un usuario con el nombre " + username);
        }
        Dueno dueno = new Dueno();
        aplicar(dueno, req);
        dueno = duenoRepository.save(dueno);

        StringBuilder clave = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            clave.append(ALFABETO.charAt(AZAR.nextInt(ALFABETO.length())));
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode(clave));
        usuario.setNombreCompleto(dueno.getNombre());
        usuario.setRol(Rol.USUARIO);
        usuario.setActivo(true);
        usuario.setDueno(dueno);
        usuarioRepository.save(usuario);

        eventPublisher.publishEvent(new CorreoEvent(dueno.getEmail(), "Tu cuenta en Vet-Care",
                "Hola " + dueno.getNombre() + ",\n\nTe registramos en Vet-Care y creamos tu cuenta para que veas "
                        + "tus mascotas, tus citas y sus vacunas:\n\n  Usuario: " + username
                        + "\n  Clave temporal: " + clave + "\n\nCámbiala la primera vez que entres.\n\nVet-Care"));

        return new DuenoCreadoResponse(dueno.getId(), dueno.getNombre(), dueno.getDocumento(),
                dueno.getTelefono(), dueno.getEmail(), dueno.getDireccion(), username, clave.toString());
    }

    @Transactional
    public DuenoResponse actualizar(Long id, DuenoRequest req) {
        exigirPropio(id);
        Dueno dueno = buscar(id);
        String documento = req.documento().trim();
        if (duenoRepository.existsByDocumentoAndIdNot(documento, id)) {
            throw new ConflictException("Ya existe otro dueño con el documento " + documento);
        }
        aplicar(dueno, req);
        return toResponse(duenoRepository.save(dueno));
    }

    public DuenoResponse obtener(Long id) {
        exigirPropio(id);
        return toResponse(buscar(id));
    }

    /** Sin filtros lista todos; con documento busca exacto; con nombre busca por coincidencia parcial. */
    public List<DuenoResponse> listar(String documento, String nombre) {
        usuarioActual.duenoRestringido().ifPresent(propio -> {
            throw new ForbiddenException("No tienes permiso para listar dueños");
        });
        String doc = Textos.limpiar(documento);
        String nom = Textos.limpiar(nombre);
        List<Dueno> resultado;
        if (doc != null) {
            resultado = duenoRepository.findByDocumento(doc).map(List::of).orElseGet(List::of);
        } else if (nom != null) {
            resultado = duenoRepository.findByNombreContainingIgnoreCase(nom);
        } else {
            resultado = duenoRepository.findAll();
        }
        return resultado.stream().map(d -> toResponse(d)).toList();
    }

    /** Un USUARIO solo puede ver y editar su propia ficha. */
    private void exigirPropio(Long id) {
        usuarioActual.duenoRestringido().ifPresent(propio -> {
            if (!propio.equals(id)) {
                throw new ForbiddenException("Solo puedes ver y editar tus propios datos");
            }
        });
    }

    private Dueno buscar(Long id) {
        return duenoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Dueño no encontrado (id=" + id + ")"));
    }

    private void aplicar(Dueno dueno, DuenoRequest req) {
        dueno.setNombre(req.nombre().trim());
        dueno.setDocumento(req.documento().trim());
        dueno.setTelefono(req.telefono().trim());
        dueno.setEmail(Textos.limpiar(req.email()));
        dueno.setDireccion(req.direccion().trim());
    }
}
