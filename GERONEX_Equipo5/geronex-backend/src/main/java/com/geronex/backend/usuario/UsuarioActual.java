package com.geronex.backend.usuario;

import com.geronex.backend.error.ApiException;
import com.geronex.backend.rol.Roles;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Obtiene la cuenta del dueno del token. Si la cuenta ya no existe o no esta ACTIVA,
 * el token deja de servir aunque no haya vencido.
 * Debe usarse dentro de un metodo @Transactional (el rol se carga de forma perezosa).
 */
@Component
public class UsuarioActual {

    private final UsuarioRepository usuarioRepository;

    public UsuarioActual(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario obtener(Jwt jwt) {
        Integer id = Integer.valueOf(jwt.getSubject());
        return usuarioRepository.findById(id)
                .filter(u -> Usuario.ESTATUS_ACTIVO.equals(u.getEstatus()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "La sesión ya no es válida"));
    }

    public static boolean esCliente(Usuario usuario) {
        return Roles.CLIENTE.equals(usuario.getRol().getNombre());
    }

    /** Nombre para mostrar: el del perfil o, si esta vacio, el nombre de usuario. */
    public static String nombreVisible(Usuario usuario) {
        if (usuario.getPerfil() != null && usuario.getPerfil().getNombre() != null
                && !usuario.getPerfil().getNombre().isBlank()) {
            return usuario.getPerfil().getNombre();
        }
        return usuario.getNombreUsuario();
    }
}
