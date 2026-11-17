package com.geronex.backend.agente;

import com.geronex.backend.dto.AgenteDto.ActualizarAgenteRequest;
import com.geronex.backend.dto.AgenteDto.AgenteResponse;
import com.geronex.backend.error.ApiException;
import com.geronex.backend.usuario.Perfil;
import com.geronex.backend.usuario.Usuario;
import com.geronex.backend.usuario.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Consultas, cambios y bajas de agentes (solo ADMINISTRADOR; las altas se hacen
 * al crear una cuenta con rol AGENTE en AdminService).
 * El nombre y el correo se mantienen iguales en agente, perfil y usuario.
 */
@Service
public class AgenteService {

    private final AgenteRepository agenteRepository;
    private final UsuarioRepository usuarioRepository;

    public AgenteService(AgenteRepository agenteRepository, UsuarioRepository usuarioRepository) {
        this.agenteRepository = agenteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<AgenteResponse> listar() {
        return agenteRepository.findAllByOrderByNombreAsc().stream().map(AgenteResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public AgenteResponse obtener(Integer idAgente) {
        return AgenteResponse.desde(buscar(idAgente));
    }

    @Transactional
    public AgenteResponse actualizar(Integer idAgente, ActualizarAgenteRequest solicitud) {
        Agente agente = buscar(idAgente);
        Usuario usuario = agente.getUsuario();
        String correo = solicitud.correo().trim().toLowerCase(Locale.ROOT);

        if (agenteRepository.existsByCorreoAndIdAgenteNot(correo, idAgente)
                || usuarioRepository.existsByCorreoAndIdUsuarioNot(correo, usuario.getIdUsuario())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ese correo ya está registrado");
        }
        String nombre = solicitud.nombre().trim();
        String telefono = solicitud.telefono() == null || solicitud.telefono().isBlank()
                ? null : solicitud.telefono().trim();

        agente.setNombre(nombre);
        agente.setCorreo(correo);
        agente.setTelefono(telefono);

        usuario.setCorreo(correo);
        if (usuario.getPerfil() == null) {
            Perfil perfil = new Perfil();
            perfil.setUsuario(usuario);
            usuario.setPerfil(perfil);
        }
        usuario.getPerfil().setNombre(nombre);
        return AgenteResponse.desde(agente);
    }

    /** Baja logica: INACTIVO tambien desactiva su cuenta, asi ya no puede iniciar sesion. */
    @Transactional
    public AgenteResponse cambiarEstatus(Integer idAgente, String estatus) {
        Agente agente = buscar(idAgente);
        agente.setEstatus(estatus);
        agente.getUsuario().setEstatus(Agente.ESTATUS_ACTIVO.equals(estatus)
                ? Usuario.ESTATUS_ACTIVO : Usuario.ESTATUS_INACTIVO);
        return AgenteResponse.desde(agente);
    }

    private Agente buscar(Integer idAgente) {
        return agenteRepository.findById(idAgente)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El agente no existe"));
    }
}
