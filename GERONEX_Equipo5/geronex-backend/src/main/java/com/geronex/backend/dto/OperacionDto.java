package com.geronex.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Solicitudes y respuestas del flujo: solicitud -> cotizacion -> reservacion. */
public final class OperacionDto {

    private OperacionDto() {
    }

    // ------------------------------------------------------------------ entradas

    public record CrearSolicitudRequest(

            @NotBlank(message = "El destino es obligatorio")
            @Size(max = 100, message = "El destino no puede tener más de 100 caracteres")
            String destino,

            @NotNull(message = "La fecha de inicio es obligatoria")
            @Future(message = "La fecha de inicio debe ser posterior a hoy")
            LocalDate fechaInicio,

            @NotNull(message = "La fecha de regreso es obligatoria")
            LocalDate fechaFin,

            @NotNull(message = "El número de personas es obligatorio")
            @Min(value = 1, message = "Debe viajar al menos 1 persona")
            @Max(value = 50, message = "El máximo es 50 personas por solicitud")
            Integer numPersonas,

            @PositiveOrZero(message = "El presupuesto no puede ser negativo")
            @Digits(integer = 8, fraction = 2, message = "El presupuesto no es válido")
            BigDecimal presupuesto,

            @Size(max = 500, message = "Las necesidades no pueden tener más de 500 caracteres")
            String necesidades) {
    }

    public record DetalleRequest(

            @NotNull(message = "Falta el servicio")
            Integer idServicio,

            @NotNull(message = "Falta la cantidad")
            @Min(value = 1, message = "La cantidad mínima es 1")
            @Max(value = 100, message = "La cantidad máxima es 100")
            Integer cantidad) {
    }

    public record CrearCotizacionRequest(

            @NotNull(message = "Falta la solicitud")
            Integer idSolicitud,

            @Size(max = 500, message = "Las notas no pueden tener más de 500 caracteres")
            String notas,

            @NotEmpty(message = "Agrega al menos un servicio a la cotización")
            List<@Valid DetalleRequest> detalles) {
    }

    public record RechazarReservacionRequest(

            @NotBlank(message = "Escribe el motivo del rechazo")
            @Size(max = 255, message = "El motivo no puede tener más de 255 caracteres")
            String motivo) {
    }

    // ------------------------------------------------------------------ salidas

    public record DetalleResponse(
            Integer idServicio,
            String servicio,
            String categoria,
            String proveedor,
            String unidad,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal) {
    }

    public record CotizacionResponse(
            Integer idCotizacion,
            Integer idSolicitud,
            String agente,
            BigDecimal total,
            String notas,
            String estatus,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaRespuesta,
            List<DetalleResponse> detalles,
            /** Solo si el cliente la acepto: datos de la reservacion que se creo. */
            Integer idReservacion,
            String folio,
            String estatusReservacion) {
    }

    public record SolicitudResponse(
            Integer idSolicitud,
            String clienteUsuario,
            String clienteNombre,
            String clienteCorreo,
            String agente,
            String destino,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer numPersonas,
            BigDecimal presupuesto,
            String necesidades,
            String estatus,
            LocalDateTime fechaCreacion,
            List<CotizacionResponse> cotizaciones) {
    }

    /** Es tambien el comprobante: trae todo lo de la reservacion, el cliente y el viaje. */
    public record ReservacionResponse(
            Integer idReservacion,
            String folio,
            String estatus,
            BigDecimal total,
            BigDecimal cargoCancelacion,
            String motivoRechazo,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaValidacion,
            LocalDateTime fechaCancelacion,
            String responsable,
            String clienteUsuario,
            String clienteNombre,
            String clienteCorreo,
            Integer idSolicitud,
            Integer idCotizacion,
            String agente,
            String destino,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer numPersonas,
            List<DetalleResponse> detalles,
            /** Ultimo dia en que el cliente todavia puede cancelar. */
            LocalDate fechaLimiteCancelacion,
            boolean puedeCancelar) {
    }
}
