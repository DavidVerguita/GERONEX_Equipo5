package com.example.geronex;

/** Configuracion central. Si cambia la direccion del backend, se cambia SOLO aqui. */
public final class Config {

    /**
     * Sale de app/build.gradle.kts segun la variante que se compile:
     *  - debug: http://localhost:8080/api (telefono por USB con "adb reverse tcp:8080 tcp:8080")
     *  - azure: http://20.69.91.86/api (la VM de Azure)
     * Emulador: http://10.0.2.2:8080/api. Produccion: la URL real con https://
     */
    public static final String BASE_URL = BuildConfig.BASE_URL;

    private Config() {
    }
}
