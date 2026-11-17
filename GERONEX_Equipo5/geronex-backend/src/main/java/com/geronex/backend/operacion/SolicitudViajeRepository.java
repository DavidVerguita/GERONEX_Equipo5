package com.geronex.backend.operacion;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SolicitudViajeRepository extends JpaRepository<SolicitudViaje, Integer> {

    List<SolicitudViaje> findByClienteIdUsuarioOrderByFechaCreacionDesc(Integer idUsuario);

    /** Las mas antiguas primero: el agente atiende en orden de llegada. */
    List<SolicitudViaje> findByEstatusOrderByFechaCreacionAsc(String estatus);

    List<SolicitudViaje> findAllByOrderByFechaCreacionDesc();

    /** Bloquea la solicitud: evita que dos agentes la coticen al mismo tiempo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitudViaje s where s.idSolicitud = :id")
    Optional<SolicitudViaje> bloquear(@Param("id") Integer id);
}
