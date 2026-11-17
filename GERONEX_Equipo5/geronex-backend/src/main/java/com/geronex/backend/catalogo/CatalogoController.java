package com.geronex.backend.catalogo;

import com.geronex.backend.dto.CatalogoDto.CategoriaResponse;
import com.geronex.backend.dto.CatalogoDto.ServicioResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoints publicos del catalogo (no requieren token). */
@RestController
@RequestMapping("/api")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/categorias")
    public List<CategoriaResponse> categorias() {
        return catalogoService.categorias();
    }

    /** Ejemplos: /api/servicios  /api/servicios?categoria=HOTEL  /api/servicios?texto=cancun */
    @GetMapping("/servicios")
    public List<ServicioResponse> servicios(@RequestParam(required = false) String categoria,
                                            @RequestParam(required = false) String texto) {
        return catalogoService.servicios(categoria, texto);
    }

    @GetMapping("/servicios/{id}")
    public ServicioResponse servicio(@PathVariable Integer id) {
        return catalogoService.servicio(id);
    }
}
