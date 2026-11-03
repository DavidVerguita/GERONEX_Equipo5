package com.geronex.backend.auth;

import com.geronex.backend.usuario.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Crea los tokens JWT firmados. El token solo lleva el id (subject) y el rol. */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final long expiracionMinutos;

    public JwtService(JwtEncoder encoder, @Value("${geronex.jwt.expiracion-minutos}") long expiracionMinutos) {
        this.encoder = encoder;
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("geronex-backend")
                .issuedAt(ahora)
                .expiresAt(ahora.plus(expiracionMinutos, ChronoUnit.MINUTES))
                .subject(String.valueOf(usuario.getIdUsuario()))
                .claim("rol", usuario.getRol().getNombre())
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }

    public long segundosDeVigencia() {
        return expiracionMinutos * 60;
    }
}
