package com.geronex.backend.operacion;

import com.geronex.backend.dto.OperacionDto.CotizacionResponse;
import com.geronex.backend.dto.OperacionDto.DetalleResponse;
import com.geronex.backend.dto.OperacionDto.ReservacionResponse;
import com.geronex.backend.dto.OperacionDto.SolicitudResponse;
import com.geronex.backend.usuario.Usuario;
import com.geronex.backend.usuario.UsuarioActual;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Convierte las entidades en las respuestas JSON.
 * Todos sus metodos deben llamarse dentro de una transaccion (relaciones perezosas).
 */
@Component
public class Mapeador {

    private final CotizacionRepository cotizacionRepository;
    private final ReservacionRepository reservacionRepository;

    public Mapeador(CotizacionRepository cotizacionRepository, ReservacionRepository reservacionRepository) {
        this.cotizacionRepository = cotizacionRepository;
        this.reservacionRepository = reservacionRepository;
    }

    public SolicitudResponse solicitud(SolicitudViaje s) {
        Usuario cliente = s.getCliente();
        List<CotizacionResponse> cotizaciones = cotizacionRepository
                .findBySolicitudIdSolicitudOrderByFechaCreacionDesc(s.getIdSolicitud())
                .stream().map(this::cotizacion).toList();
        return new SolicitudResponse(
                s.getIdSolicitud(),
                cliente.getNombreUsuario(),
                UsuarioActual.nombreVisible(cliente),
                cliente.getCorreo(),
                s.getAgente() != null ? s.getAgente().getNombre() : null,
                s.getDestino(),
                s.getFechaInicio(),
                s.getFechaFin(),
                s.getNumPersonas(),
                s.getPresupuesto(),
                s.getNecesidades(),
                s.getEstatus(),
                s.getFechaCreacion(),
                cotizaciones);
    }

    public CotizacionResponse cotizacion(Cotizacion c) {
        Optional<Reservacion> reservacion = reservacionRepository.findByCotizacionIdCotizacion(c.getIdCotizacion());
        return new CotizacionResponse(
                c.getIdCotizacion(),
                c.getSolicitud().getIdSolicitud(),
                c.getAgente().getNombre(),
                c.getTotal(),
                c.getNotas(),
                c.getEstatus(),
                c.getFechaCreacion(),
                c.getFechaRespuesta(),
                detalles(c),
                reservacion.map(Reservacion::getIdReservacion).orElse(null),
                reservacion.map(Reservacion::getFolio).orElse(null),
                reservacion.map(Reservacion::getEstatus).orElse(null));
    }

    public ReservacionResponse reservacion(Reservacion r) {
        Cotizacion c = r.getCotizacion();
        SolicitudViaje s = c.getSolicitud();
        Usuario cliente = r.getCliente();
        LocalDate hoy = LocalDate.now();
        boolean puedeCancelar = ReglasCancelacion.estatusCancelable(r.getEstatus())
                && ReglasCancelacion.enTiempo(s.getFechaInicio(), hoy);
        return new ReservacionResponse(
                r.getIdReservacion(),
                r.getFolio(),
                r.getEstatus(),
                r.getTotal(),
                r.getCargoCancelacion(),
                r.getMotivoRechazo(),
                r.getFechaCreacion(),
                r.getFechaValidacion(),
                r.getFechaCancelacion(),
                r.getResponsable() != null ? UsuarioActual.nombreVisible(r.getResponsable()) : null,
                cliente.getNombreUsuario(),
                UsuarioActual.nombreVisible(cliente),
                cliente.getCorreo(),
                s.getIdSolicitud(),
                c.getIdCotizacion(),
                c.getAgente().getNombre(),
                s.getDestino(),
                s.getFechaInicio(),
                s.getFechaFin(),
                s.getNumPersonas(),
                detalles(c),
                ReglasCancelacion.fechaLimite(s.getFechaInicio()),
                puedeCancelar);
    }

    private List<DetalleResponse> detalles(Cotizacion c) {
        return c.getDetalles().stream().map(d -> new DetalleResponse(
                d.getServicio().getIdServicio(),
                d.getServicio().getNombre(),
                d.getServicio().getCategoria().getNombre(),
                d.getServicio().getProveedor().getNombre(),
                d.getServicio().getUnidad(),
                d.getCantidad(),
                d.getPrecioUnitario(),
                d.getSubtotal())).toList();
    }
}
