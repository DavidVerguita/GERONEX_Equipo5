package com.geronex.backend.usuario;

import com.geronex.backend.dto.ActualizarCuentaRequest;
import com.geronex.backend.dto.CambiarPasswordRequest;
import com.geronex.backend.dto.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints privados (requieren token). Todos trabajan sobre "me":
 * la cuenta del dueno del token. No existe ninguna ruta con el id de otro usuario.
 */
@RestController
@RequestMapping("/api/usuarios/me")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public UsuarioResponse consultar(@AuthenticationPrincipal Jwt jwt) {
        return usuarioService.obtenerCuenta(idDe(jwt));
    }

    @PutMapping
    public UsuarioResponse actualizar(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody ActualizarCuentaRequest solicitud) {
        return usuarioService.actualizarCuenta(idDe(jwt), solicitud);
    }

    @PatchMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPassword(@AuthenticationPrincipal Jwt jwt,
                                @Valid @RequestBody CambiarPasswordRequest solicitud) {
        usuarioService.cambiarPassword(idDe(jwt), solicitud);
    }

    private Integer idDe(Jwt jwt) {
        return Integer.valueOf(jwt.getSubject());
    }
}
