package com.geronex.backend.error;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Convierte las excepciones en respuestas JSON coherentes, sin exponer detalles internos. */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorRespuesta> manejarApi(ApiException ex) {
        return respuesta(ex.getStatus(), ex.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return respuesta(HttpStatus.BAD_REQUEST, "Hay datos inválidos en la solicitud", errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> manejarJsonInvalido(HttpMessageNotReadableException ex) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "Algún dato tiene un formato incorrecto (las fechas van como AAAA-MM-DD y los números sin letras)",
                Map.of());
    }

    /** Por ejemplo /api/solicitudes/abc en lugar de un numero. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorRespuesta> manejarTipo(MethodArgumentTypeMismatchException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "El identificador de la ruta no es válido", Map.of());
    }

    /** Dos operaciones al mismo tiempo sobre la misma fila (por ejemplo, el mismo cupo). */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErrorRespuesta> manejarBloqueo(PessimisticLockingFailureException ex) {
        return respuesta(HttpStatus.CONFLICT, "Otra operación estaba usando estos datos. Inténtalo de nuevo.", Map.of());
    }

    /** Red de seguridad: si dos solicitudes simultaneas chocan con un UNIQUE de MySQL. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuesta> manejarIntegridad(DataIntegrityViolationException ex) {
        return respuesta(HttpStatus.CONFLICT, "Ya existe un registro con esos datos", Map.of());
    }

    private ResponseEntity<ErrorRespuesta> respuesta(HttpStatus status, String mensaje, Map<String, String> errores) {
        return ResponseEntity.status(status)
                .body(new ErrorRespuesta(status.value(), mensaje, errores, Instant.now()));
    }
}
