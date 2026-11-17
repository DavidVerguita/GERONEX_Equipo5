package com.geronex.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Solo contiene los campos que el usuario PUEDE cambiar. No incluye rol, estatus ni contrasena. */
public record ActualizarCuentaRequest(

        @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 254, message = "El correo es demasiado largo")
        String correo) {
}
