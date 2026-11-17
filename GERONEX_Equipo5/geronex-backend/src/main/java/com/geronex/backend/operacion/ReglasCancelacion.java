package com.geronex.backend.operacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Regla de cancelacion acordada por el equipo:
 * - Se puede cancelar si faltan al menos 3 dias naturales para la fecha de inicio del viaje.
 * - Si la reservacion ya estaba CONFIRMADA se registra un cargo del 5% del total (no hay cobro real).
 * - Si todavia estaba POR_VALIDAR no hay cargo, porque aun no se habia apartado cupo.
 */
public final class ReglasCancelacion {

    public static final int DIAS_ANTICIPACION = 3;
    public static final BigDecimal PORCENTAJE_CARGO = new BigDecimal("0.05");

    private ReglasCancelacion() {
    }

    public static LocalDate fechaLimite(LocalDate fechaInicioViaje) {
        return fechaInicioViaje.minusDays(DIAS_ANTICIPACION);
    }

    public static boolean estatusCancelable(String estatus) {
        return Reservacion.POR_VALIDAR.equals(estatus) || Reservacion.CONFIRMADA.equals(estatus);
    }

    public static boolean enTiempo(LocalDate fechaInicioViaje, LocalDate hoy) {
        return !hoy.isAfter(fechaLimite(fechaInicioViaje));
    }

    public static BigDecimal cargo(String estatusAntes, BigDecimal total) {
        if (!Reservacion.CONFIRMADA.equals(estatusAntes)) {
            return BigDecimal.ZERO.setScale(2);
        }
        return total.multiply(PORCENTAJE_CARGO).setScale(2, RoundingMode.HALF_UP);
    }
}
