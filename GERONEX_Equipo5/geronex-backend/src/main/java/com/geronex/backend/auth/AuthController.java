package com.geronex.backend.auth;

import com.geronex.backend.dto.AuthResponse;
import com.geronex.backend.dto.LoginRequest;
import com.geronex.backend.dto.RegistroRequest;
import com.geronex.backend.dto.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Capa Controller: endpoints publicos. Solo recibe, valida (@Valid) y delega en el servicio. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registro(@Valid @RequestBody RegistroRequest solicitud) {
        return authService.registrar(solicitud);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest solicitud) {
        return authService.iniciarSesion(solicitud);
    }
}
