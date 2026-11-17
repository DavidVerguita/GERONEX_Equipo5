package com.geronex.backend.error;

import org.springframework.http.HttpStatus;

/** Error "esperado" de negocio con el codigo HTTP que debe devolverse. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
