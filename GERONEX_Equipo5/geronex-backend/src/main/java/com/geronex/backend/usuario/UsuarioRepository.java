package com.geronex.backend.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** Capa Repository: Spring genera la implementacion a partir del nombre de los metodos. */
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByCorreo(String correo);

    /** Para editar el correo: existe otro usuario (distinto al actual) con ese correo? */
    boolean existsByCorreoAndIdUsuarioNot(String correo, Integer idUsuario);

    /** Todas las cuentas con su rol, para la pagina del administrador. */
    @Query("select u from Usuario u join fetch u.rol order by u.idUsuario")
    List<Usuario> findAllConRol();
}
