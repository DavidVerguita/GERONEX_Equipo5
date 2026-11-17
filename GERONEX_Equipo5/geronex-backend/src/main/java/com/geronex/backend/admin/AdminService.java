package com.geronex.backend.admin;

import com.geronex.backend.agente.Agente;
import com.geronex.backend.agente.AgenteRepository;
import com.geronex.backend.auth.AuthService;
import com.geronex.backend.dto.AdminDto.CrearCuentaRequest;
import com.geronex.backend.dto.UsuarioResponse;
import com.geronex.backend.error.ApiException;
import com.geronex.backend.rol.Roles;
import com.geronex.backend.usuario.Usuario;
import com.geronex.backend.usuario.UsuarioActual;
import com.geronex.backend.usuario.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** El administrador crea cuentas de empleados y activa o desactiva cuentas. */
@Service
public class AdminService {

    private final AuthService authService;
    private final UsuarioRepository usuarioRepository;
    private final AgenteRepository agenteRepository;

    public AdminService(AuthService authService, UsuarioRepository usuarioRepository,
                        AgenteRepository agenteRepository) {
        this.authService = authService;
        this.usuarioRepository = usuarioRepository;
        this.agenteRepository = agenteRepository;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAllConRol().stream().map(UsuarioResponse::desde).toList();
    }

    /** Si el rol es AGENTE, tambien crea su fila en la tabla agente (alta de agente). */
    @Transactional
    public UsuarioResponse crear(CrearCuentaRequest solicitud) {
        Usuario usuario = authService.crearCuenta(solicitud.nombreUsuario(), solicitud.correo(),
                solicitud.contrasena(), solicitud.rol(), solicitud.nombre());

        if (Roles.AGENTE.equals(solicitud.rol())) {
            if (agenteRepository.existsByCorreo(usuario.getCorreo())) {
                throw new ApiException(HttpStatus.CONFLICT, "Ya existe un agente con ese correo");
            }
            Agente agente = new Agente();
            agente.setUsuario(usuario);
            agente.setNombre(UsuarioActual.nombreVisible(usuario));
            agente.setCorreo(usuario.getCorreo());
            agente.setTelefono(solicitud.telefono() == null || solicitud.telefono().isBlank()
                    ? null : solicitud.telefono().trim());
            agente.setEstatus(Agente.ESTATUS_ACTIVO);
            agenteRepository.save(agente);
        }
        return UsuarioResponse.desde(usuario);
    }

    @Transactional
    public UsuarioResponse cambiarEstatus(Integer idAdmin, Integer idUsuario, String estatus) {
        if (idAdmin.equals(idUsuario)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No puedes cambiar el estatus de tu propia cuenta");
        }
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La cuenta no existe"));
        usuario.setEstatus(estatus);
        // Mantener igual el estatus del agente (ahi solo existe ACTIVO o INACTIVO)
        agenteRepository.findByUsuarioIdUsuario(idUsuario).ifPresent(a ->
                a.setEstatus(Usuario.ESTATUS_ACTIVO.equals(estatus) ? Agente.ESTATUS_ACTIVO : Agente.ESTATUS_INACTIVO));
        return UsuarioResponse.desde(usuario);
    }
}
