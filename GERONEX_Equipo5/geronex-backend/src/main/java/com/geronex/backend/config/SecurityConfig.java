package com.geronex.backend.config;

import com.geronex.backend.rol.Roles;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Seguridad de la API: rutas publicas/privadas, JWT, hash de contrasenas y CORS.
 * La API es "stateless": no guarda sesiones en el servidor; cada peticion trae su token.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filtroDeSeguridad(HttpSecurity http) throws Exception {
        http
                // CSRF protege sesiones con cookies. Aqui no hay cookies (token en cabecera), se desactiva.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // --- Publicas ---
                        .requestMatchers(HttpMethod.POST, "/api/auth/registro", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/salud", "/api/categorias",
                                "/api/servicios", "/api/servicios/*").permitAll()

                        // --- Administracion de cuentas y agentes ---
                        .requestMatchers("/api/admin/**").hasRole(Roles.ADMINISTRADOR)
                        .requestMatchers(HttpMethod.GET, "/api/agentes", "/api/agentes/*")
                                .hasAnyRole(Roles.ADMINISTRADOR, Roles.SUPERVISOR)
                        .requestMatchers("/api/agentes/**").hasRole(Roles.ADMINISTRADOR)

                        // --- Solicitudes de viaje ---
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes").hasRole(Roles.CLIENTE)
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes/mias").hasRole(Roles.CLIENTE)
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes")
                                .hasAnyRole(Roles.AGENTE, Roles.SUPERVISOR, Roles.ADMINISTRADOR, Roles.AUDITOR)
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes/*").authenticated()

                        // --- Cotizaciones ---
                        .requestMatchers(HttpMethod.POST, "/api/cotizaciones").hasRole(Roles.AGENTE)
                        .requestMatchers(HttpMethod.POST, "/api/cotizaciones/*/aceptar",
                                "/api/cotizaciones/*/rechazar").hasRole(Roles.CLIENTE)

                        // --- Reservaciones ---
                        .requestMatchers(HttpMethod.GET, "/api/reservaciones/mias").hasRole(Roles.CLIENTE)
                        .requestMatchers(HttpMethod.GET, "/api/reservaciones")
                                .hasAnyRole(Roles.RESPONSABLE_RESERVACIONES, Roles.SUPERVISOR,
                                        Roles.ADMINISTRADOR, Roles.AUDITOR)
                        .requestMatchers(HttpMethod.GET, "/api/reservaciones/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reservaciones/*/confirmar",
                                "/api/reservaciones/*/rechazar")
                                .hasAnyRole(Roles.RESPONSABLE_RESERVACIONES, Roles.SUPERVISOR)
                        .requestMatchers(HttpMethod.POST, "/api/reservaciones/*/cancelar").hasRole(Roles.CLIENTE)

                        // --- Todo lo demas (por ejemplo /api/usuarios/me): cualquier sesion valida ---
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorJwt()))
                        .authenticationEntryPoint(SecurityConfig::sinSesion)
                        .accessDeniedHandler(SecurityConfig::sinPermiso))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(SecurityConfig::sinSesion)
                        .accessDeniedHandler(SecurityConfig::sinPermiso));
        return http.build();
    }

    /** 401 con el mismo formato JSON que el resto de los errores. */
    private static void sinSesion(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex)
            throws IOException {
        escribirError(res, 401, "Tu sesión no es válida o ya venció. Inicia sesión de nuevo.");
    }

    /** 403: hay sesion, pero el rol no tiene permiso para esa ruta. */
    private static void sinPermiso(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex)
            throws IOException {
        escribirError(res, 403, "No tienes permiso para realizar esta acción.");
    }

    private static void escribirError(HttpServletResponse res, int estado, String mensaje) throws IOException {
        res.setStatus(estado);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"estado\":" + estado + ",\"mensaje\":\"" + mensaje
                + "\",\"errores\":{},\"fecha\":\"" + Instant.now() + "\"}");
    }

    /** Convierte la claim "rol" del token en la autoridad ROLE_<rol> de Spring Security. */
    private JwtAuthenticationConverter convertidorJwt() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("rol");
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(autoridades);
        return convertidor;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey claveJwt(@Value("${geronex.jwt.secret}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey claveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey claveJwt) {
        return NimbusJwtDecoder.withSecretKey(claveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** Solo los origenes web listados en geronex.cors.origenes pueden llamar a la API desde un navegador. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${geronex.cors.origenes}") List<String> origenes) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(origenes);
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }
}
