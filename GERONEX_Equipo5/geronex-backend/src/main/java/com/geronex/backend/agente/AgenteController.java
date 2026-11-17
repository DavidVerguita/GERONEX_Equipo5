package com.geronex.backend.agente;

import com.geronex.backend.dto.AgenteDto.ActualizarAgenteRequest;
import com.geronex.backend.dto.AgenteDto.AgenteResponse;
import com.geronex.backend.dto.AgenteDto.CambiarEstatusRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestion de agentes. Quien puede usar cada ruta se define en SecurityConfig:
 * consultar = ADMINISTRADOR o SUPERVISOR; cambiar y dar de baja = solo ADMINISTRADOR.
 * Alta: POST /api/admin/usuarios con rol AGENTE.
 */
@RestController
@RequestMapping("/api/agentes")
public class AgenteController {

    private final AgenteService agenteService;

    public AgenteController(AgenteService agenteService) {
        this.agenteService = agenteService;
    }

    @GetMapping
    public List<AgenteResponse> listar() {
        return agenteService.listar();
    }

    @GetMapping("/{id}")
    public AgenteResponse obtener(@PathVariable Integer id) {
        return agenteService.obtener(id);
    }

    @PutMapping("/{id}")
    public AgenteResponse actualizar(@PathVariable Integer id, @Valid @RequestBody ActualizarAgenteRequest solicitud) {
        return agenteService.actualizar(id, solicitud);
    }

    @PatchMapping("/{id}/estatus")
    public AgenteResponse cambiarEstatus(@PathVariable Integer id, @Valid @RequestBody CambiarEstatusRequest solicitud) {
        return agenteService.cambiarEstatus(id, solicitud.estatus());
    }
}
