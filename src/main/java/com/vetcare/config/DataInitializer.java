package com.vetcare.config;

import com.vetcare.model.Usuario;
import com.vetcare.model.enums.Rol;
import com.vetcare.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Si no hay ningún usuario, crea tres de demostración para poder probar los roles desde el primer arranque. */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${vetcare.seed.usuarios-demo:true}")
    private boolean sembrarUsuariosDemo;

    @Override
    public void run(String... args) {
        if (!sembrarUsuariosDemo || usuarioRepository.count() > 0) {
            return;
        }
        crear("admin", "Admin2026*", "Administrador", Rol.ADMINISTRADOR);
        crear("recepcion", "Recepcion2026*", "Recepcionista", Rol.RECEPCIONISTA);
        crear("veterinario", "Veterinario2026*", "Veterinario", Rol.VETERINARIO);
        log.warn("Se crearon los usuarios de demostración admin, recepcion y veterinario. "
                + "Cambia sus contraseñas (o desactiva vetcare.seed.usuarios-demo) antes de usar el sistema de verdad.");
    }

    private void crear(String username, String password, String nombre, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setNombreCompleto(nombre);
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }
}
