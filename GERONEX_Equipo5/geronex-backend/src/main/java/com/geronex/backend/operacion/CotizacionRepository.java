package com.geronex.backend.operacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Integer> {

    List<Cotizacion> findBySolicitudIdSolicitudOrderByFechaCreacionDesc(Integer idSolicitud);
}
