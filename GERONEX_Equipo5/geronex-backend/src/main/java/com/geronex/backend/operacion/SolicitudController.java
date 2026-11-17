package com.geronex.backend.operacion;

import com.geronex.backend.dto.OperacionDto.CrearSolicitudRequest;
import com.geronex.backend.dto.OperacionDto.SolicitudResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Los roles permitidos en cada ruta estan en SecurityConfig. */
@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    /** CLIENTE: crear una solicitud de viaje. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudResponse crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CrearSolicitudRequest datos) {
        return solicitudService.crear(jwt, datos);
    }

    /** CLIENTE: mis solicitudes, con sus cotizaciones. */
    @GetMapping("/mias")
    public List<SolicitudResponse> mias(@AuthenticationPrincipal Jwt jwt) {
        return solicitudService.mias(jwt);
    }

    /** Empleados: todas, o filtradas. Ejemplo: /api/solicitudes?estatus=NUEVA */
    @GetMapping
    public List<SolicitudResponse> listar(@AuthenticationPrincipal Jwt jwt,
                                          @RequestParam(required = false) String estatus) {
        return solicitudService.listar(jwt, estatus);
    }

    /** El cliente dueno o un empleado. */
    @GetMapping("/{id}")
    public SolicitudResponse obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return solicitudService.obtener(jwt, id);
    }
}
