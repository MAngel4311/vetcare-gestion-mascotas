package com.vetcare.security;

import com.vetcare.exception.ForbiddenException;
import com.vetcare.exception.NotFoundException;
import com.vetcare.model.Mascota;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Datos de quien hace la petición. Se usa para que un USUARIO (dueño) solo vea y gestione lo suyo:
 * los demás roles no tienen esa restricción.
 */
@Component
public class UsuarioActual {

    private static final String ROL_USUARIO = "ROLE_USUARIO";

    /** Si quien consulta tiene rol USUARIO devuelve el id de su dueño; para los demás roles, vacío (sin restricción). */
    public Optional<Long> duenoRestringido() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        boolean esUsuario = auth.getAuthorities().stream().anyMatch(a -> ROL_USUARIO.equals(a.getAuthority()));
        if (!esUsuario) {
            return Optional.empty();
        }
        Object id = jwt.getClaims().get("duenoId");
        if (id == null) {
            throw new ForbiddenException("Tu cuenta no está vinculada a un dueño");
        }
        return Optional.of(id instanceof Number n ? n.longValue() : Long.parseLong(id.toString()));
    }

    /** Si quien consulta es VETERINARIO devuelve su id de veterinario; para los demás roles, vacío (sin restricción). */
    public Optional<Long> veterinarioRestringido() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        boolean esVeterinario = auth.getAuthorities().stream().anyMatch(a -> "ROLE_VETERINARIO".equals(a.getAuthority()));
        if (!esVeterinario) {
            return Optional.empty();
        }
        Object id = jwt.getClaims().get("veterinarioId");
        if (id == null) {
            throw new ForbiddenException("Tu cuenta no está vinculada a un veterinario. Pide al administrador que la vincule");
        }
        return Optional.of(id instanceof Number n ? n.longValue() : Long.parseLong(id.toString()));
    }

    /** Si es un VETERINARIO y la cita es de otro veterinario, responde 404. */
    public void exigirVeterinario(Long veterinarioDelRecurso, String mensajeNoEncontrado) {
        Optional<Long> propio = veterinarioRestringido();
        if (propio.isPresent() && !propio.get().equals(veterinarioDelRecurso)) {
            throw new NotFoundException(mensajeNoEncontrado);
        }
    }

    /** Si es un USUARIO y el recurso es de otro dueño, responde 404 (no revela que existe). */
    public void exigirDueno(Long duenoDelRecurso, String mensajeNoEncontrado) {
        Optional<Long> propio = duenoRestringido();
        if (propio.isPresent() && !propio.get().equals(duenoDelRecurso)) {
            throw new NotFoundException(mensajeNoEncontrado);
        }
    }

    public void exigirMascotaPropia(Mascota mascota) {
        exigirDueno(mascota.getDueno().getId(), "Mascota no encontrada (id=" + mascota.getId() + ")");
    }
}
