package com.example.geronex;

/** Configuracion central. Si cambia la direccion del backend, se cambia SOLO aqui. */
public final class Config {

    /**
     * Telefono fisico por USB con "adb reverse tcp:8080 tcp:8080": localhost llega a tu computadora.
     * Emulador: usa http://10.0.2.2:8080/api
     * Produccion: la URL real con https://
     */
    public static final String BASE_URL = "http://localhost:8080/api";

    private Config() {
    }
}
