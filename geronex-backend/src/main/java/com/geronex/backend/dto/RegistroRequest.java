package com.geronex.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistroRequest(

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(max = 15, message = "El nombre de usuario no puede tener más de 15 caracteres")
        String nombreUsuario,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 254, message = "El correo es demasiado largo")
        String correo,

        @NotNull(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 50, message = "La contraseña debe tener entre 8 y 50 caracteres")
        String contrasena) {
}
