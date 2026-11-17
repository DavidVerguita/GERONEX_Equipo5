package com.geronex.backend.operacion;

import com.geronex.backend.dto.OperacionDto.CotizacionResponse;
import com.geronex.backend.dto.OperacionDto.CrearCotizacionRequest;
import com.geronex.backend.dto.OperacionDto.ReservacionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Los roles permitidos en cada ruta estan en SecurityConfig. */
@RestController
@RequestMapping("/api/cotizaciones")
public class CotizacionController {

    private final CotizacionService cotizacionService;

    public CotizacionController(CotizacionService cotizacionService) {
        this.cotizacionService = cotizacionService;
    }

    /** AGENTE: enviar una cotizacion para una solicitud NUEVA. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CotizacionResponse crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CrearCotizacionRequest datos) {
        return cotizacionService.crear(jwt, datos);
    }

    /** CLIENTE: aceptar. Devuelve la reservacion creada (POR_VALIDAR) con su folio. */
    @PostMapping("/{id}/aceptar")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservacionResponse aceptar(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return cotizacionService.aceptar(jwt, id);
    }

    /** CLIENTE: rechazar. La solicitud vuelve a NUEVA. */
    @PostMapping("/{id}/rechazar")
    public CotizacionResponse rechazar(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return cotizacionService.rechazar(jwt, id);
    }
}
