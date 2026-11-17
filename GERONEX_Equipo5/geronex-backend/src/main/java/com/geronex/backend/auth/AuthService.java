package com.geronex.backend.auth;

import com.geronex.backend.dto.AuthResponse;
import com.geronex.backend.dto.LoginRequest;
import com.geronex.backend.dto.RegistroRequest;
import com.geronex.backend.dto.UsuarioResponse;
import com.geronex.backend.error.ApiException;
import com.geronex.backend.rol.Rol;
import com.geronex.backend.rol.RolRepository;
import com.geronex.backend.rol.Roles;
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

    /** Registro publico: siempre crea un CLIENTE, sin importar lo que mande el cliente. */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest solicitud) {
        Usuario usuario = crearCuenta(solicitud.nombreUsuario(), solicitud.correo(),
                solicitud.contrasena(), Roles.CLIENTE, null);
        return UsuarioResponse.desde(usuario);
    }

    /**
     * Crea una cuenta con el rol indicado. La usan el registro publico (CLIENTE)
     * y el administrador (empleados). Si nombre viene vacio, el perfil toma el nombre de usuario.
     */
    @Transactional
    public Usuario crearCuenta(String nombreUsuarioCrudo, String correoCrudo, String contrasena,
                               String nombreRol, String nombre) {
        String nombreUsuario = nombreUsuarioCrudo.trim();
        String correo = correoCrudo.trim().toLowerCase(Locale.ROOT);

        if (ContrasenaUtil.excedeLimiteBcrypt(contrasena)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "La contraseña es demasiado larga; usa menos caracteres especiales");
        }
        if (usuarioRepository.existsByNombreUsuario(nombreUsuario)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ese nombre de usuario ya está en uso");
        }
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ese correo ya está registrado");
        }

        Rol rol = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "El rol " + nombreRol + " no existe"));

        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(nombreUsuario);
        usuario.setCorreo(correo);
        usuario.setContrasena(passwordEncoder.encode(contrasena));
        usuario.setRol(rol);
        usuario.setEstatus(Usuario.ESTATUS_ACTIVO);

        Perfil perfil = new Perfil();
        perfil.setUsuario(usuario);
        perfil.setNombre(nombre == null || nombre.isBlank() ? nombreUsuario : nombre.trim());
        usuario.setPerfil(perfil);

        return usuarioRepository.save(usuario); // el cascade guarda tambien el perfil
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
