package com.geronex.backend.dto;

import java.math.BigDecimal;

/** Respuestas del catalogo publico. */
public final class CatalogoDto {

    private CatalogoDto() {
    }

    public record CategoriaResponse(Integer idCategoria, String nombre, String descripcion) {
    }

    public record ServicioResponse(
            Integer idServicio,
            String nombre,
            String descripcion,
            String destino,
            String categoria,
            String proveedor,
            BigDecimal precio,
            String unidad,
            Integer cupoDisponible) {
    }
}
