package com.geronex.backend.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ServicioRepository extends JpaRepository<Servicio, Integer> {

    /** Servicios activos con su categoria y proveedor en una sola consulta. */
    @Query("select s from Servicio s join fetch s.categoria c join fetch s.proveedor p "
            + "where s.estatus = 'ACTIVO' order by s.destino, c.nombre, s.nombre")
    List<Servicio> findActivosConDetalle();
}
