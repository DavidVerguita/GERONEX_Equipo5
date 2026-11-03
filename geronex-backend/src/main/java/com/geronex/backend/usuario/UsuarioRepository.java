package com.geronex.backend.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Capa Repository: Spring genera la implementacion a partir del nombre de los metodos. */
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByCorreo(String correo);

    /** Para editar el correo: existe otro usuario (distinto al actual) con ese correo? */
    boolean existsByCorreoAndIdUsuarioNot(String correo, Integer idUsuario);
}
