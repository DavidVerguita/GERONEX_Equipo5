package com.geronex.backend.agente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgenteRepository extends JpaRepository<Agente, Integer> {

    Optional<Agente> findByUsuarioIdUsuario(Integer idUsuario);

    List<Agente> findAllByOrderByNombreAsc();

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdAgenteNot(String correo, Integer idAgente);
}
