package com.geronex.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CambiarPasswordRequest(

        @NotNull(message = "La contraseña actual es obligatoria")
        @Size(max = 200, message = "La contraseña actual es incorrecta")
        String contrasenaActual,

        @NotNull(message = "La contraseña nueva es obligatoria")
        @Size(min = 8, max = 50, message = "La contraseña nueva debe tener entre 8 y 50 caracteres")
        String contrasenaNueva) {
}
