package com.vetcare.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.vetcare.model.enums.Rol;
import com.vetcare.security.RestSecurityHandlers;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Seguridad sin sesiones: el usuario hace login, recibe un token JWT y lo envía en cada petición.
 * Quién puede hacer qué (según los requisitos de stakeholder):
 *  - RECEPCIONISTA: dueños, mascotas y citas (programar, cancelar, reprogramar).
 *  - VETERINARIO: registro clínico, historial y vacunas.
 *  - ADMINISTRADOR: todo, más reportes, veterinarios y usuarios.
 *  - USUARIO (dueño): se registra solo y ve/gestiona únicamente sus datos, mascotas y citas.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String RECEPCIONISTA = Rol.RECEPCIONISTA.name();
    private static final String VETERINARIO = Rol.VETERINARIO.name();
    private static final String ADMINISTRADOR = Rol.ADMINISTRADOR.name();
    private static final String USUARIO = Rol.USUARIO.name();

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           @Qualifier("corsConfigurationSource") CorsConfigurationSource cors,
                                           RestSecurityHandlers manejadores,
                                           JwtAuthenticationConverter conversor) throws Exception {
        http
                .cors(c -> c.configurationSource(cors))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Públicas
                        .requestMatchers("/error", "/api/auth/login", "/api/auth/registro",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()

                        // Cualquier usuario con sesión: cambiar su propia contraseña
                        .requestMatchers(HttpMethod.PUT, "/api/auth/password").authenticated()

                        // Solo administrador
                        .requestMatchers("/api/usuarios/**", "/api/reportes/**", "/api/notificaciones/**").hasRole(ADMINISTRADOR)
                        .requestMatchers(HttpMethod.POST, "/api/veterinarios", "/api/vacunas/recordatorios")
                        .hasRole(ADMINISTRADOR)
                        .requestMatchers(HttpMethod.PUT, "/api/veterinarios/*").hasRole(ADMINISTRADOR)

                        // Veterinario (y administrador): información clínica
                        .requestMatchers(HttpMethod.GET, "/api/mascotas/*/historial")
                        .hasAnyRole(VETERINARIO, ADMINISTRADOR, USUARIO)
                        .requestMatchers(HttpMethod.POST, "/api/citas/*/registro-clinico")
                        .hasAnyRole(VETERINARIO, ADMINISTRADOR)
                        .requestMatchers(HttpMethod.POST, "/api/mascotas/*/vacunas")
                        .hasAnyRole(VETERINARIO, ADMINISTRADOR)

                        // Recepcionista (y administrador): dueños, mascotas y citas
                        .requestMatchers(HttpMethod.POST, "/api/duenos").hasAnyRole(RECEPCIONISTA, ADMINISTRADOR)
                        .requestMatchers(HttpMethod.PUT, "/api/duenos/*")
                        .hasAnyRole(RECEPCIONISTA, ADMINISTRADOR, USUARIO)
                        .requestMatchers(HttpMethod.POST, "/api/mascotas")
                        .hasAnyRole(RECEPCIONISTA, ADMINISTRADOR, USUARIO)
                        .requestMatchers(HttpMethod.PUT, "/api/mascotas/*")
                        .hasAnyRole(RECEPCIONISTA, ADMINISTRADOR, USUARIO)
                        .requestMatchers(HttpMethod.POST, "/api/citas").hasAnyRole(RECEPCIONISTA, ADMINISTRADOR)
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/*/cancelar", "/api/citas/*/confirmar")
                        .hasAnyRole(RECEPCIONISTA, ADMINISTRADOR)
                        .requestMatchers(HttpMethod.PUT, "/api/citas/*/reprogramar")
                        .hasAnyRole(RECEPCIONISTA, ADMINISTRADOR)

                        // Consultas que también puede hacer el USUARIO (dueño); el servicio filtra a lo suyo
                        .requestMatchers(HttpMethod.GET, "/api/auth/me", "/api/duenos/*", "/api/mascotas",
                                "/api/mascotas/*", "/api/mascotas/*/vacunas", "/api/citas", "/api/citas/*")
                        .authenticated()

                        // Consultas (GET) restantes: solo el personal de la clínica
                        .requestMatchers(HttpMethod.GET, "/api/**")
                        .hasAnyRole(RECEPCIONISTA, VETERINARIO, ADMINISTRADOR)

                        // Todo lo demás, prohibido
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversor))
                        .authenticationEntryPoint(manejadores)
                        .accessDeniedHandler(manejadores))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(manejadores)
                        .accessDeniedHandler(manejadores));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey jwtSecretKey(@Value("${vetcare.jwt.secret}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("vetcare.jwt.secret debe tener al menos 32 caracteres");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** Lee los roles del claim "roles" del token y les agrega el prefijo ROLE_ (lo que espera hasRole). */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("roles");
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }
}
