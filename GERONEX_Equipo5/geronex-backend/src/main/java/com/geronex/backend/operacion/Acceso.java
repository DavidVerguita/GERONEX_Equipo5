package com.geronex.backend.operacion;

import com.geronex.backend.error.ApiException;
import com.geronex.backend.usuario.Usuario;
import com.geronex.backend.usuario.UsuarioActual;
import org.springframework.http.HttpStatus;

/** Reglas de quien puede ver una solicitud o una reservacion. */
final class Acceso {

    private Acceso() {
    }

    /**
     * Un cliente solo ve lo suyo; los empleados ven todo.
     * A un cliente que pide algo ajeno se le responde 404, para no revelar que existe.
     */
    static void verificarDuenoOEmpleado(Usuario usuario, Usuario dueno, String recurso) {
        if (UsuarioActual.esCliente(usuario) && !usuario.getIdUsuario().equals(dueno.getIdUsuario())) {
            throw new ApiException(HttpStatus.NOT_FOUND, recurso + " no existe");
        }
    }

    /** Para acciones que solo puede hacer el cliente dueno (aceptar, rechazar, cancelar). */
    static void verificarDueno(Usuario usuario, Usuario dueno, String recurso) {
        if (!usuario.getIdUsuario().equals(dueno.getIdUsuario())) {
            throw new ApiException(HttpStatus.NOT_FOUND, recurso + " no existe");
        }
    }
}
