package com.geronex.backend.dto;

public record AuthResponse(
        String token,
        String tipo,
        long expiraEnSegundos,
        UsuarioResponse usuario) {
}
