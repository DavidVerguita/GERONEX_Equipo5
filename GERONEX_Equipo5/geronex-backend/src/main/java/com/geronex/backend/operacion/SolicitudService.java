package com.geronex.backend.operacion;

import com.geronex.backend.dto.OperacionDto.CrearSolicitudRequest;
import com.geronex.backend.dto.OperacionDto.SolicitudResponse;
import com.geronex.backend.error.ApiException;
import com.geronex.backend.usuario.Usuario;
import com.geronex.backend.usuario.UsuarioActual;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/** Solicitudes de viaje: el cliente las crea y los agentes las atienden. */
@Service
public class SolicitudService {

    private static final Set<String> ESTATUS_VALIDOS =
            Set.of(SolicitudViaje.NUEVA, SolicitudViaje.COTIZADA, SolicitudViaje.ACEPTADA);

    private final SolicitudViajeRepository solicitudRepository;
    private final UsuarioActual usuarioActual;
    private final Mapeador mapeador;

    public SolicitudService(SolicitudViajeRepository solicitudRepository, UsuarioActual usuarioActual,
                            Mapeador mapeador) {
        this.solicitudRepository = solicitudRepository;
        this.usuarioActual = usuarioActual;
        this.mapeador = mapeador;
    }

    @Transactional
    public SolicitudResponse crear(Jwt jwt, CrearSolicitudRequest datos) {
        Usuario cliente = usuarioActual.obtener(jwt);
        if (datos.fechaFin().isBefore(datos.fechaInicio())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha de regreso no puede ser antes de la fecha de inicio");
        }
        SolicitudViaje s = new SolicitudViaje();
        s.setCliente(cliente);
        s.setDestino(datos.destino().trim());
        s.setFechaInicio(datos.fechaInicio());
        s.setFechaFin(datos.fechaFin());
        s.setNumPersonas(datos.numPersonas());
        s.setPresupuesto(datos.presupuesto());
        s.setNecesidades(datos.necesidades() == null || datos.necesidades().isBlank()
                ? null : datos.necesidades().trim());
        s.setEstatus(SolicitudViaje.NUEVA);
        solicitudRepository.save(s);
        return mapeador.solicitud(s);
    }

    @Transactional(readOnly = true)
    public List<SolicitudResponse> mias(Jwt jwt) {
        Usuario cliente = usuarioActual.obtener(jwt);
        return solicitudRepository.findByClienteIdUsuarioOrderByFechaCreacionDesc(cliente.getIdUsuario())
                .stream().map(mapeador::solicitud).toList();
    }

    /** Para empleados. estatus es opcional (NUEVA, COTIZADA o ACEPTADA). */
    @Transactional(readOnly = true)
    public List<SolicitudResponse> listar(Jwt jwt, String estatus) {
        usuarioActual.obtener(jwt);
        List<SolicitudViaje> lista;
        if (estatus == null || estatus.isBlank()) {
            lista = solicitudRepository.findAllByOrderByFechaCreacionDesc();
        } else if (ESTATUS_VALIDOS.contains(estatus)) {
            lista = solicitudRepository.findByEstatusOrderByFechaCreacionAsc(estatus);
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Estatus de solicitud no válido");
        }
        return lista.stream().map(mapeador::solicitud).toList();
    }

    @Transactional(readOnly = true)
    public SolicitudResponse obtener(Jwt jwt, Integer idSolicitud) {
        Usuario usuario = usuarioActual.obtener(jwt);
        SolicitudViaje s = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La solicitud no existe"));
        Acceso.verificarDuenoOEmpleado(usuario, s.getCliente(), "La solicitud");
        return mapeador.solicitud(s);
    }
}
