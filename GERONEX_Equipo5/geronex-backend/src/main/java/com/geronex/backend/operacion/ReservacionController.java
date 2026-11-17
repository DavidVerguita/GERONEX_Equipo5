package com.geronex.backend.operacion;

import com.geronex.backend.dto.OperacionDto.RechazarReservacionRequest;
import com.geronex.backend.dto.OperacionDto.ReservacionResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Los roles permitidos en cada ruta estan en SecurityConfig. */
@RestController
@RequestMapping("/api/reservaciones")
public class ReservacionController {

    private final ReservacionService reservacionService;

    public ReservacionController(ReservacionService reservacionService) {
        this.reservacionService = reservacionService;
    }

    /** CLIENTE: mis reservaciones. */
    @GetMapping("/mias")
    public List<ReservacionResponse> mias(@AuthenticationPrincipal Jwt jwt) {
        return reservacionService.mias(jwt);
    }

    /** Empleados: todas, o filtradas. Ejemplo: /api/reservaciones?estatus=POR_VALIDAR */
    @GetMapping
    public List<ReservacionResponse> listar(@AuthenticationPrincipal Jwt jwt,
                                            @RequestParam(required = false) String estatus) {
        return reservacionService.listar(jwt, estatus);
    }

    /** Comprobante: el cliente dueno o un empleado. */
    @GetMapping("/{id}")
    public ReservacionResponse obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return reservacionService.obtener(jwt, id);
    }

    /** RESPONSABLE_RESERVACIONES o SUPERVISOR: validar disponibilidad y confirmar. */
    @PostMapping("/{id}/confirmar")
    public ReservacionResponse confirmar(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return reservacionService.confirmar(jwt, id);
    }

    /** RESPONSABLE_RESERVACIONES o SUPERVISOR: rechazar por falta de disponibilidad. */
    @PostMapping("/{id}/rechazar")
    public ReservacionResponse rechazar(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id,
                                        @Valid @RequestBody RechazarReservacionRequest datos) {
        return reservacionService.rechazar(jwt, id, datos.motivo());
    }

    /** CLIENTE: cancelar (3 dias de anticipacion; cargo del 5% si ya estaba confirmada). */
    @PostMapping("/{id}/cancelar")
    public ReservacionResponse cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return reservacionService.cancelar(jwt, id);
    }
}
