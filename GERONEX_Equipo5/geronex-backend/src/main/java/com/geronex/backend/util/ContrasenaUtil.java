package com.geronex.backend.util;

import java.nio.charset.StandardCharsets;

public final class ContrasenaUtil {

    /** BCrypt solo procesa los primeros 72 BYTES de la contrasena. */
    public static final int LIMITE_BYTES_BCRYPT = 72;

    private ContrasenaUtil() {
    }

    public static boolean excedeLimiteBcrypt(String contrasena) {
        return contrasena.getBytes(StandardCharsets.UTF_8).length > LIMITE_BYTES_BCRYPT;
    }
}
