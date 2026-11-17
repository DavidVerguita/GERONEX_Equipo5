package com.example.geronex;

/** Ilustracion de cada destino (las mismas de la web, en formato vectorial de Android). */
public final class Destinos {

    private Destinos() {
    }

    public static int imagen(String destino) {
        String d = Formato.normalizar(destino);
        if (d.contains("cancun")) return R.drawable.dest_cancun;
        if (d.contains("cabos")) return R.drawable.dest_los_cabos;
        if (d.contains("cdmx") || d.contains("ciudad de mexico")) return R.drawable.dest_cdmx;
        if (d.contains("oaxaca")) return R.drawable.dest_oaxaca;
        if (d.contains("chihuahua")) return R.drawable.dest_chihuahua;
        return R.drawable.dest_generico;
    }
}
