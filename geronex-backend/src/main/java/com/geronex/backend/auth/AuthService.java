package com.geronex.backend.auth;

import com.geronex.backend.dto.AuthResponse;
import com.geronex.backend.dto.LoginRequest;
import com.geronex.backend.dto.RegistroRequest;
import com.geronex.backend.dto.UsuarioResponse;
import com.geronex.backend.error.ApiException;
import com.geronex.backend.rol.Rol;
import com.geronex.backend.rol.RolRepository;
import com.geronex.backend.usuario.Perfil;
import com.geronex.backend.usuario.Usuario;
import com.geronex.backend.usuario.UsuarioRepository;
import com.geronex.backend.util.ContrasenaUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Capa Service: reglas de negocio de registro e inicio de sesion. */
@Service
public class AuthService {

    private static final String ROL_REGISTRO_PUBLICO = "CLIENTE";
    private static final String MENSAJE_LOGIN = "Usuario o contraseña incorrectos";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /** Hash de relleno: se compara cuando el usuario no existe para que tarde lo mismo. */
    private final String hashDeRelleno;

    public AuthService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.hashDeRelleno = passwordEncoder.encode("contrasena-de-relleno");
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest solicitud) {
        String nombreUsuario = solicitud.nombreUsuario().trim();
        String correo = solicitud.correo().trim().toLowerCase(Locale.ROOT);

        if (ContrasenaUtil.excedeLimiteBcrypt(solicitud.contrasena())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "La contraseña es demasiado larga; usa menos caracteres especiales");
        }
        if (usuarioRepository.existsByNombreUsuario(nombreUsuario)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ese nombre de usuario ya está en uso");
        }
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ese correo ya está registrado");
        }

        Rol rolCliente = rolRepository.findByNombre(ROL_REGISTRO_PUBLICO)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el rol CLIENTE en la base de datos (ejecuta 01_esquema_cuentas.sql)"));

        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(nombreUsuario);
        usuario.setCorreo(correo);
        usuario.setContrasena(passwordEncoder.encode(solicitud.contrasena()));
        usuario.setRol(rolCliente);
        usuario.setEstatus(Usuario.ESTATUS_ACTIVO);

        Perfil perfil = new Perfil();
        perfil.setUsuario(usuario);
        usuario.setPerfil(perfil);

        usuarioRepository.save(usuario); // el cascade guarda tambien el perfil
        return UsuarioResponse.desde(usuario);
    }

    @Transactional(readOnly = true)
    public AuthResponse iniciarSesion(LoginRequest solicitud) {
        String nombreUsuario = solicitud.nombreUsuario().trim();
        String contrasena = solicitud.contrasena();

        // Una contrasena de mas de 72 bytes nunca pudo registrarse
        if (ContrasenaUtil.excedeLimiteBcrypt(contrasena)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, MENSAJE_LOGIN);
        }

        Usuario usuario = usuarioRepository.findByNombreUsuario(nombreUsuario).orElse(null);
        String hash = usuario != null ? usuario.getContrasena() : hashDeRelleno;
        boolean coincide = passwordEncoder.matches(contrasena, hash);

        // Mismo mensaje para "no existe", "clave mala" o "cuenta no activa": no revela cual fue
        if (usuario == null || !coincide || !Usuario.ESTATUS_ACTIVO.equals(usuario.getEstatus())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, MENSAJE_LOGIN);
        }

        return new AuthResponse(
                jwtService.generarToken(usuario),
                "Bearer",
                jwtService.segundosDeVigencia(),
                UsuarioResponse.desde(usuario));
    }
}
