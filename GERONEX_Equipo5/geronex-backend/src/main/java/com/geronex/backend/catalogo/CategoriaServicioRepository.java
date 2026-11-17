package com.geronex.backend.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaServicioRepository extends JpaRepository<CategoriaServicio, Integer> {

    List<CategoriaServicio> findAllByOrderByNombreAsc();
}
