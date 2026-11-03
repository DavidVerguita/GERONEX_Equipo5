package com.geronex.backend.usuario;

import com.geronex.backend.dto.ActualizarCuentaRequest;
import com.geronex.backend.dto.CambiarPasswordRequest;
import com.geronex.backend.dto.UsuarioResponse;
import com.geronex.backend.error.ApiException;
import com.geronex.backend.util.ContrasenaUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Capa Service: operaciones sobre la cuenta del usuario autenticado.
 * El id SIEMPRE viene del token (lo pone el controlador), nunca del cuerpo de la peticion.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerCuenta(Integer idUsuario) {
        return UsuarioResponse.desde(obtenerActivo(idUsuario));
    }

    @Transactional
    public UsuarioResponse actualizarCuenta(Integer idUsuario, ActualizarCuentaRequest solicitud) {
        Usuario usuario = obtenerActivo(idUsuario);
        String correo = solicitud.correo().trim().toLowerCase(Locale.ROOT);

        if (!correo.equals(usuario.getCorreo())
                && usuarioRepository.existsByCorreoAndIdUsuarioNot(correo, idUsuario)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ese correo ya está registrado");
        }
        usuario.setCorreo(correo);

        if (usuario.getPerfil() == null) {
            Perfil perfil = new Perfil();
            perfil.setUsuario(usuario);
            usuario.setPerfil(perfil);
        }
        String nombre = solicitud.nombre() == null || solicitud.nombre().isBlank()
                ? null
                : solicitud.nombre().trim();
        usuario.getPerfil().setNombre(nombre);

        usuarioRepository.save(usuario);
        return UsuarioResponse.desde(usuario);
    }

    @Transactional
    public void cambiarPassword(Integer idUsuario, CambiarPasswordRequest solicitud) {
        Usuario usuario = obtenerActivo(idUsuario);
        String actual = solicitud.contrasenaActual();
        String nueva = solicitud.contrasenaNueva();

        // 400 (no 401) para que el cliente no confunda esto con una sesion vencida
        if (ContrasenaUtil.excedeLimiteBcrypt(actual)
                || !passwordEncoder.matches(actual, usuario.getContrasena())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta");
        }
        if (ContrasenaUtil.excedeLimiteBcrypt(nueva)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "La contraseña nueva es demasiado larga; usa menos caracteres especiales");
        }
        if (passwordEncoder.matches(nueva, usuario.getContrasena())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña nueva debe ser distinta a la actual");
        }

        usuario.setContrasena(passwordEncoder.encode(nueva));
        usuarioRepository.save(usuario);
    }

    /** Si el usuario ya no existe o no esta ACTIVO, el token deja de servir. */
    private Usuario obtenerActivo(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .filter(u -> Usuario.ESTATUS_ACTIVO.equals(u.getEstatus()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "La sesión ya no es válida"));
    }
}
