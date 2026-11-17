package com.geronex.backend.catalogo;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, Integer> {

    Optional<Disponibilidad> findByServicioIdServicio(Integer idServicio);

    /**
     * SELECT ... FOR UPDATE: bloquea la fila hasta que termine la transaccion.
     * Asi dos confirmaciones al mismo tiempo no pueden apartar el ultimo lugar dos veces.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Disponibilidad d where d.servicio.idServicio = :idServicio")
    Optional<Disponibilidad> bloquearPorServicio(@Param("idServicio") Integer idServicio);
}
