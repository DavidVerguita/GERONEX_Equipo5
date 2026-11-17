package com.geronex.backend.operacion;

import com.geronex.backend.catalogo.Disponibilidad;
import com.geronex.backend.catalogo.DisponibilidadRepository;
import com.geronex.backend.dto.OperacionDto.ReservacionResponse;
import com.geronex.backend.error.ApiException;
import com.geronex.backend.usuario.Usuario;
import com.geronex.backend.usuario.UsuarioActual;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Reservaciones: el responsable valida disponibilidad y confirma o rechaza;
 * el cliente consulta su comprobante y puede cancelar segun ReglasCancelacion.
 */
@Service
public class ReservacionService {

    private static final Set<String> ESTATUS_VALIDOS = Set.of(Reservacion.POR_VALIDAR,
            Reservacion.CONFIRMADA, Reservacion.RECHAZADA, Reservacion.CANCELADA);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReservacionRepository reservacionRepository;
    private final DisponibilidadRepository disponibilidadRepository;
    private final UsuarioActual usuarioActual;
    private final Mapeador mapeador;

    public ReservacionService(ReservacionRepository reservacionRepository,
                              DisponibilidadRepository disponibilidadRepository,
                              UsuarioActual usuarioActual, Mapeador mapeador) {
        this.reservacionRepository = reservacionRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.usuarioActual = usuarioActual;
        this.mapeador = mapeador;
    }

    @Transactional(readOnly = true)
    public List<ReservacionResponse> mias(Jwt jwt) {
        Usuario cliente = usuarioActual.obtener(jwt);
        return reservacionRepository.findByClienteIdUsuarioOrderByFechaCreacionDesc(cliente.getIdUsuario())
                .stream().map(mapeador::reservacion).toList();
    }

    /** Para empleados. estatus opcional. */
    @Transactional(readOnly = true)
    public List<ReservacionResponse> listar(Jwt jwt, String estatus) {
        usuarioActual.obtener(jwt);
        List<Reservacion> lista;
        if (estatus == null || estatus.isBlank()) {
            lista = reservacionRepository.findAllByOrderByFechaCreacionDesc();
        } else if (ESTATUS_VALIDOS.contains(estatus)) {
            lista = reservacionRepository.findByEstatusOrderByFechaCreacionAsc(estatus);
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Estatus de reservación no válido");
        }
        return lista.stream().map(mapeador::reservacion).toList();
    }

    /** Comprobante: el cliente dueno o cualquier empleado. */
    @Transactional(readOnly = true)
    public ReservacionResponse obtener(Jwt jwt, Integer idReservacion) {
        Usuario usuario = usuarioActual.obtener(jwt);
        Reservacion r = buscar(idReservacion);
        Acceso.verificarDuenoOEmpleado(usuario, r.getCliente(), "La reservación");
        return mapeador.reservacion(r);
    }

    /**
     * Valida disponibilidad y la descuenta. Todo pasa en una transaccion con las filas
     * de disponibilidad bloqueadas: si un servicio no alcanza, no se descuenta nada.
     */
    @Transactional
    public ReservacionResponse confirmar(Jwt jwt, Integer idReservacion) {
        Usuario responsable = usuarioActual.obtener(jwt);
        Reservacion r = buscarPorValidar(idReservacion);
        LocalDate inicioViaje = r.getCotizacion().getSolicitud().getFechaInicio();
        if (!inicioViaje.isAfter(LocalDate.now())) {
            throw new ApiException(HttpStatus.CONFLICT, "El viaje empieza el " + inicioViaje.format(FECHA)
                    + " y esa fecha ya llegó: no se puede confirmar. Rechaza la reservación con ese motivo.");
        }

        // Se bloquea en orden de id para que dos confirmaciones no se esperen entre si (deadlock)
        List<DetalleCotizacion> detalles = r.getCotizacion().getDetalles().stream()
                .sorted(Comparator.comparing(d -> d.getServicio().getIdServicio()))
                .toList();
        for (DetalleCotizacion d : detalles) {
            Disponibilidad disp = disponibilidadRepository.bloquearPorServicio(d.getServicio().getIdServicio())
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                            "\"" + d.getServicio().getNombre() + "\" no tiene disponibilidad registrada"));
            if (disp.getCupoDisponible() < d.getCantidad()) {
                throw new ApiException(HttpStatus.CONFLICT, "No hay disponibilidad de \""
                        + d.getServicio().getNombre() + "\": se piden " + d.getCantidad()
                        + " y quedan " + disp.getCupoDisponible() + ". Rechaza la reservación con ese motivo.");
            }
            disp.descontar(d.getCantidad());
        }

        r.setEstatus(Reservacion.CONFIRMADA);
        r.setResponsable(responsable);
        r.setFechaValidacion(LocalDateTime.now());
        return mapeador.reservacion(r);
    }

    /** Sin disponibilidad: la solicitud vuelve a NUEVA para que el agente proponga otra cosa. */
    @Transactional
    public ReservacionResponse rechazar(Jwt jwt, Integer idReservacion, String motivo) {
        Usuario responsable = usuarioActual.obtener(jwt);
        Reservacion r = buscarPorValidar(idReservacion);

        r.setEstatus(Reservacion.RECHAZADA);
        r.setMotivoRechazo(motivo.trim());
        r.setResponsable(responsable);
        r.setFechaValidacion(LocalDateTime.now());
        r.getCotizacion().getSolicitud().setEstatus(SolicitudViaje.NUEVA);
        return mapeador.reservacion(r);
    }

    /** El cliente cancela con al menos 3 dias de anticipacion. */
    @Transactional
    public ReservacionResponse cancelar(Jwt jwt, Integer idReservacion) {
        Usuario cliente = usuarioActual.obtener(jwt);
        Reservacion r = bloquear(idReservacion);
        Acceso.verificarDueno(cliente, r.getCliente(), "La reservación");

        String estatusAntes = r.getEstatus();
        if (!ReglasCancelacion.estatusCancelable(estatusAntes)) {
            throw new ApiException(HttpStatus.CONFLICT, "Esta reservación ya no se puede cancelar");
        }
        LocalDate inicio = r.getCotizacion().getSolicitud().getFechaInicio();
        if (!ReglasCancelacion.enTiempo(inicio, LocalDate.now())) {
            throw new ApiException(HttpStatus.CONFLICT, "Solo puedes cancelar con al menos "
                    + ReglasCancelacion.DIAS_ANTICIPACION + " días de anticipación. Tu viaje empieza el "
                    + inicio.format(FECHA) + " y el último día para cancelar era el "
                    + ReglasCancelacion.fechaLimite(inicio).format(FECHA) + ".");
        }

        // Si ya estaba confirmada, el cupo se habia descontado: se devuelve
        if (Reservacion.CONFIRMADA.equals(estatusAntes)) {
            List<DetalleCotizacion> detalles = r.getCotizacion().getDetalles().stream()
                    .sorted(Comparator.comparing(d -> d.getServicio().getIdServicio()))
                    .toList();
            for (DetalleCotizacion d : detalles) {
                disponibilidadRepository.bloquearPorServicio(d.getServicio().getIdServicio())
                        .ifPresent(disp -> disp.liberar(d.getCantidad()));
            }
        }

        r.setEstatus(Reservacion.CANCELADA);
        r.setCargoCancelacion(ReglasCancelacion.cargo(estatusAntes, r.getTotal()));
        r.setFechaCancelacion(LocalDateTime.now());
        return mapeador.reservacion(r);
    }

    private Reservacion buscar(Integer idReservacion) {
        return reservacionRepository.findById(idReservacion)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La reservación no existe"));
    }

    private Reservacion bloquear(Integer idReservacion) {
        return reservacionRepository.bloquear(idReservacion)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La reservación no existe"));
    }

    private Reservacion buscarPorValidar(Integer idReservacion) {
        Reservacion r = bloquear(idReservacion);
        if (!Reservacion.POR_VALIDAR.equals(r.getEstatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Esta reservación ya fue validada o cancelada");
        }
        return r;
    }
}
