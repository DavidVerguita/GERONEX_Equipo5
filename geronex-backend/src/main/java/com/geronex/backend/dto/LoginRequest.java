package com.geronex.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(max = 50, message = "Usuario o contraseña incorrectos")
        String nombreUsuario,

        @NotNull(message = "La contraseña es obligatoria")
        @Size(max = 200, message = "Usuario o contraseña incorrectos")
        String contrasena) {
}
