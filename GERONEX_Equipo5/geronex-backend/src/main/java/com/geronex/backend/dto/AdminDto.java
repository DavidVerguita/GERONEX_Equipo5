package com.geronex.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Solicitudes del administrador para gestionar cuentas. */
public final class AdminDto {

    private AdminDto() {
    }

    public record CrearCuentaRequest(

            @NotBlank(message = "El nombre de usuario es obligatorio")
            @Size(max = 15, message = "El nombre de usuario no puede tener más de 15 caracteres")
            String nombreUsuario,

            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no tiene un formato válido")
            @Size(max = 254, message = "El correo es demasiado largo")
            String correo,

            @NotNull(message = "La contraseña es obligatoria")
            @Size(min = 8, max = 50, message = "La contraseña debe tener entre 8 y 50 caracteres")
            String contrasena,

            @NotBlank(message = "El rol es obligatorio")
            @Pattern(regexp = "ADMINISTRADOR|SUPERVISOR|AGENTE|RESPONSABLE_RESERVACIONES|RESPONSABLE_PROVEEDORES|AUDITOR|CLIENTE",
                    message = "El rol no es válido")
            String rol,

            @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
            String nombre,

            /** Solo se usa si el rol es AGENTE. */
            @Pattern(regexp = "^$|^[0-9]{10}$", message = "El teléfono debe tener exactamente 10 dígitos")
            String telefono) {
    }

    public record CambiarEstatusCuentaRequest(

            @NotBlank(message = "El estatus es obligatorio")
            @Pattern(regexp = "ACTIVO|INACTIVO|BLOQUEADO", message = "El estatus debe ser ACTIVO, INACTIVO o BLOQUEADO")
            String estatus) {
    }
}
