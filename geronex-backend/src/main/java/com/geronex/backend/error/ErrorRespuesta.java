package com.geronex.backend.error;

import java.time.Instant;
import java.util.Map;

/** Formato unico de error para web y Android. */
public record ErrorRespuesta(
        int estado,
        String mensaje,
        Map<String, String> errores,
        Instant fecha) {
}
