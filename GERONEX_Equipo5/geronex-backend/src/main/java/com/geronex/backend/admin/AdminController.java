package com.geronex.backend.admin;

import com.geronex.backend.dto.AdminDto.CambiarEstatusCuentaRequest;
import com.geronex.backend.dto.AdminDto.CrearCuentaRequest;
import com.geronex.backend.dto.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Solo ADMINISTRADOR (regla en SecurityConfig). */
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return adminService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody CrearCuentaRequest solicitud) {
        return adminService.crear(solicitud);
    }

    @PatchMapping("/{id}/estatus")
    public UsuarioResponse cambiarEstatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id,
                                          @Valid @RequestBody CambiarEstatusCuentaRequest solicitud) {
        return adminService.cambiarEstatus(Integer.valueOf(jwt.getSubject()), id, solicitud.estatus());
    }
}
