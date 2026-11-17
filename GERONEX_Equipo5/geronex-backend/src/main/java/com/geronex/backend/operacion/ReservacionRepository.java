package com.geronex.backend.operacion;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservacionRepository extends JpaRepository<Reservacion, Integer> {

    Optional<Reservacion> findByCotizacionIdCotizacion(Integer idCotizacion);

    List<Reservacion> findByClienteIdUsuarioOrderByFechaCreacionDesc(Integer idUsuario);

    List<Reservacion> findByEstatusOrderByFechaCreacionAsc(String estatus);

    List<Reservacion> findAllByOrderByFechaCreacionDesc();

    boolean existsByFolio(String folio);

    /** Bloquea la reservacion: evita que se confirme y se cancele al mismo tiempo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservacion r where r.idReservacion = :id")
    Optional<Reservacion> bloquear(@Param("id") Integer id);
}
