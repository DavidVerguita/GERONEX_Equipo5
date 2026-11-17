package com.geronex.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del backend. @SpringBootApplication activa la configuracion
 * automatica y busca los componentes (controladores, servicios, repositorios)
 * dentro del paquete com.geronex.backend y sus subpaquetes.
 */
@SpringBootApplication
public class GeronexApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeronexApplication.class, args);
    }
}
