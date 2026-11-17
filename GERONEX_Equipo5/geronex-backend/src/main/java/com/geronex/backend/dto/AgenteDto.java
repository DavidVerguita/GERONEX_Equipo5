package com.geronex.backend.dto;

import com.geronex.backend.agente.Agente;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Solicitudes y respuestas de la gestion de agentes. */
public final class AgenteDto {

    private AgenteDto() {
    }

    public record AgenteResponse(
            Integer idAgente,
            Integer idUsuario,
            String nombreUsuario,
            String nombre,
            String correo,
            String telefono,
            String estatus) {

        /** Debe llamarse dentro de una transaccion. */
        public static AgenteResponse desde(Agente a) {
            return new AgenteResponse(a.getIdAgente(), a.getUsuario().getIdUsuario(),
                    a.getUsuario().getNombreUsuario(), a.getNombre(), a.getCorreo(),
                    a.getTelefono(), a.getEstatus());
        }
    }

    public record ActualizarAgenteRequest(

            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
            String nombre,

            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no tiene un formato válido")
            @Size(max = 254, message = "El correo es demasiado largo")
            String correo,

            @Pattern(regexp = "^$|^[0-9]{10}$", message = "El teléfono debe tener exactamente 10 dígitos")
            String telefono) {
    }

    public record CambiarEstatusRequest(

            @NotBlank(message = "El estatus es obligatorio")
            @Pattern(regexp = "ACTIVO|INACTIVO", message = "El estatus debe ser ACTIVO o INACTIVO")
            String estatus) {
    }
}
