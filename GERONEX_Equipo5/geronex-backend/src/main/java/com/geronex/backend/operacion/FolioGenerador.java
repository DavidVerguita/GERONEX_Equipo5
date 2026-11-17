package com.geronex.backend.operacion;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Genera folios unicos como GX-261116-K7PQ (fecha + 4 caracteres al azar). */
@Component
public class FolioGenerador {

    /** Sin 0, O, 1, I ni L para que no se confundan al leerlos. */
    private static final String CARACTERES = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyMMdd");

    private final SecureRandom azar = new SecureRandom();
    private final ReservacionRepository reservacionRepository;

    public FolioGenerador(ReservacionRepository reservacionRepository) {
        this.reservacionRepository = reservacionRepository;
    }

    public String nuevo() {
        String folio;
        do {
            StringBuilder sb = new StringBuilder("GX-").append(LocalDate.now().format(FORMATO_FECHA)).append('-');
            for (int i = 0; i < 4; i++) {
                sb.append(CARACTERES.charAt(azar.nextInt(CARACTERES.length())));
            }
            folio = sb.toString();
        } while (reservacionRepository.existsByFolio(folio));
        return folio;
    }
}
