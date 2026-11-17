package com.geronex.backend.salud;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint TEMPORAL: GET /api/salud
 * Capa Controller: solo recibe la peticion y delega en el servicio.
 */
@RestController
@RequestMapping("/api/salud")
public class SaludController {

    private final SaludService saludService;

    public SaludController(SaludService saludService) {
        this.saludService = saludService;
    }

    @GetMapping
    public Map<String, Object> verificar() {
        return saludService.estadoBaseDeDatos();
    }
}
