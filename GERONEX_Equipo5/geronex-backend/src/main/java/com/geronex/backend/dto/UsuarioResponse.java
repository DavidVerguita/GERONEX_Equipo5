package com.geronex.backend.dto;

import com.geronex.backend.usuario.Usuario;

import java.time.LocalDateTime;

/** Lo que la API devuelve de una cuenta. Nunca incluye la contrasena ni su hash. */
public record UsuarioResponse(
        Integer idUsuario,
        String nombreUsuario,
        String correo,
        String nombre,
        String rol,
        String estatus,
        LocalDateTime fechaCreacion) {

    /** Debe llamarse dentro de una transaccion (el rol se carga de forma perezosa). */
    public static UsuarioResponse desde(Usuario u) {
        String nombre = u.getPerfil() != null ? u.getPerfil().getNombre() : null;
        return new UsuarioResponse(
                u.getIdUsuario(),
                u.getNombreUsuario(),
                u.getCorreo(),
                nombre,
                u.getRol().getNombre(),
                u.getEstatus(),
                u.getFechaCreacion());
    }
}
