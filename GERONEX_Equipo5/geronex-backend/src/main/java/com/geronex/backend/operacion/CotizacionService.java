package com.geronex.backend.operacion;

import com.geronex.backend.agente.Agente;
import com.geronex.backend.agente.AgenteRepository;
import com.geronex.backend.catalogo.Disponibilidad;
import com.geronex.backend.catalogo.DisponibilidadRepository;
import com.geronex.backend.catalogo.Servicio;
import com.geronex.backend.catalogo.ServicioRepository;
import com.geronex.backend.dto.OperacionDto.CotizacionResponse;
import com.geronex.backend.dto.OperacionDto.CrearCotizacionRequest;
import com.geronex.backend.dto.OperacionDto.DetalleRequest;
import com.geronex.backend.dto.OperacionDto.ReservacionResponse;
import com.geronex.backend.error.ApiException;
import com.geronex.backend.usuario.Usuario;
import com.geronex.backend.usuario.UsuarioActual;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/** El agente arma cotizaciones; el cliente las acepta o las rechaza. */
@Service
public class CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final SolicitudViajeRepository solicitudRepository;
    private final ReservacionRepository reservacionRepository;
    private final ServicioRepository servicioRepository;
    private final DisponibilidadRepository disponibilidadRepository;
    private final AgenteRepository agenteRepository;
    private final UsuarioActual usuarioActual;
    private final FolioGenerador folioGenerador;
    private final Mapeador mapeador;

    public CotizacionService(CotizacionRepository cotizacionRepository,
                             SolicitudViajeRepository solicitudRepository,
                             ReservacionRepository reservacionRepository,
                             ServicioRepository servicioRepository,
                             DisponibilidadRepository disponibilidadRepository,
                             AgenteRepository agenteRepository,
                             UsuarioActual usuarioActual,
                             FolioGenerador folioGenerador,
                             Mapeador mapeador) {
        this.cotizacionRepository = cotizacionRepository;
        this.solicitudRepository = solicitudRepository;
        this.reservacionRepository = reservacionRepository;
        this.servicioRepository = servicioRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.agenteRepository = agenteRepository;
        this.usuarioActual = usuarioActual;
        this.folioGenerador = folioGenerador;
        this.mapeador = mapeador;
    }

    /**
     * El agente cotiza una solicitud NUEVA. El total se calcula aqui con los precios
     * actuales del catalogo (el navegador nunca manda precios).
     */
    @Transactional
    public CotizacionResponse crear(Jwt jwt, CrearCotizacionRequest datos) {
        Usuario usuario = usuarioActual.obtener(jwt);
        Agente agente = agenteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .filter(a -> Agente.ESTATUS_ACTIVO.equals(a.getEstatus()))
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "Tu cuenta no está registrada como agente activo"));

        SolicitudViaje solicitud = solicitudRepository.bloquear(datos.idSolicitud())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La solicitud no existe"));
        if (!SolicitudViaje.NUEVA.equals(solicitud.getEstatus())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Esta solicitud ya tiene una cotización en curso o ya fue aceptada");
        }
        verificarFechaVigente(solicitud);

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setSolicitud(solicitud);
        cotizacion.setAgente(agente);
        cotizacion.setNotas(datos.notas() == null || datos.notas().isBlank() ? null : datos.notas().trim());
        cotizacion.setEstatus(Cotizacion.ENVIADA);

        BigDecimal total = BigDecimal.ZERO;
        Set<Integer> usados = new HashSet<>();
        for (DetalleRequest d : datos.detalles()) {
            if (!usados.add(d.idServicio())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "No repitas un servicio; mejor aumenta su cantidad");
            }
            Servicio servicio = servicioRepository.findById(d.idServicio())
                    .filter(s -> Servicio.ESTATUS_ACTIVO.equals(s.getEstatus()))
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                            "El servicio " + d.idServicio() + " no existe o no está activo"));
            int cupo = disponibilidadRepository.findByServicioIdServicio(servicio.getIdServicio())
                    .map(Disponibilidad::getCupoDisponible).orElse(0);
            if (cupo < d.cantidad()) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "No hay cupo suficiente de \"" + servicio.getNombre() + "\" (quedan " + cupo + ")");
            }

            BigDecimal subtotal = servicio.getPrecio().multiply(BigDecimal.valueOf(d.cantidad()));
            DetalleCotizacion detalle = new DetalleCotizacion();
            detalle.setServicio(servicio);
            detalle.setCantidad(d.cantidad());
            detalle.setPrecioUnitario(servicio.getPrecio());
            detalle.setSubtotal(subtotal);
            cotizacion.agregarDetalle(detalle);
            total = total.add(subtotal);
        }
        cotizacion.setTotal(total);
        cotizacionRepository.save(cotizacion);

        solicitud.setAgente(agente);
        solicitud.setEstatus(SolicitudViaje.COTIZADA);
        return mapeador.cotizacion(cotizacion);
    }

    /** El cliente acepta: nace la reservacion POR_VALIDAR con su folio. */
    @Transactional
    public ReservacionResponse aceptar(Jwt jwt, Integer idCotizacion) {
        Usuario cliente = usuarioActual.obtener(jwt);
        Cotizacion cotizacion = buscarEnviadaDelCliente(cliente, idCotizacion);
        verificarFechaVigente(cotizacion.getSolicitud());

        cotizacion.setEstatus(Cotizacion.ACEPTADA);
        cotizacion.setFechaRespuesta(LocalDateTime.now());
        cotizacion.getSolicitud().setEstatus(SolicitudViaje.ACEPTADA);

        Reservacion reservacion = new Reservacion();
        reservacion.setFolio(folioGenerador.nuevo());
        reservacion.setCotizacion(cotizacion);
        reservacion.setCliente(cliente);
        reservacion.setTotal(cotizacion.getTotal());
        reservacion.setEstatus(Reservacion.POR_VALIDAR);
        reservacionRepository.save(reservacion);
        return mapeador.reservacion(reservacion);
    }

    /** El cliente rechaza: la solicitud vuelve a NUEVA para que el agente mande otra propuesta. */
    @Transactional
    public CotizacionResponse rechazar(Jwt jwt, Integer idCotizacion) {
        Usuario cliente = usuarioActual.obtener(jwt);
        Cotizacion cotizacion = buscarEnviadaDelCliente(cliente, idCotizacion);

        cotizacion.setEstatus(Cotizacion.RECHAZADA);
        cotizacion.setFechaRespuesta(LocalDateTime.now());
        cotizacion.getSolicitud().setEstatus(SolicitudViaje.NUEVA);
        return mapeador.cotizacion(cotizacion);
    }

    /** No se cotiza ni se reserva un viaje cuya fecha de inicio ya llegó o ya pasó. */
    private static void verificarFechaVigente(SolicitudViaje solicitud) {
        if (!solicitud.getFechaInicio().isAfter(LocalDate.now())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "La fecha de inicio de este viaje ya llegó o ya pasó. El cliente debe crear una nueva solicitud.");
        }
    }

    private Cotizacion buscarEnviadaDelCliente(Usuario cliente, Integer idCotizacion) {
        Cotizacion cotizacion = cotizacionRepository.findById(idCotizacion)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La cotización no existe"));
        Acceso.verificarDueno(cliente, cotizacion.getSolicitud().getCliente(), "La cotización");
        if (!Cotizacion.ENVIADA.equals(cotizacion.getEstatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Esta cotización ya fue respondida");
        }
        return cotizacion;
    }
}
