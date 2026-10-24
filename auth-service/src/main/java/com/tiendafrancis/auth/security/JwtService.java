package com.tiendafrancis.auth.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import com.tiendafrancis.auth.bean.entity.Usuario;
import com.tiendafrancis.auth.persistence.UsuarioRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final UsuarioRepository usuarios;
    private final Clock reloj;
    private final byte[] clave;
    private final String emisor;
    private final long duracionSegundos;

    public JwtService(

            UsuarioRepository usuarios,

            Clock reloj,

            @Value("${JWT_SECRET:}")
            String secreto,

            @Value("${security.jwt.issuer:tienda-francis}")
            String emisor,

            @Value("${security.jwt.expiration-seconds:3600}")
            long duracionSegundos

    ) {

        this.usuarios = usuarios;
        this.reloj = reloj;
        this.clave = secreto.getBytes(StandardCharsets.UTF_8);
        this.emisor = emisor;
        this.duracionSegundos = duracionSegundos;

        if (clave.length < 32
                || emisor.isBlank()
                || duracionSegundos < 60
                || duracionSegundos > 86400) {

            throw new IllegalStateException(
                    "Configura JWT_SECRET (mínimo 32 bytes), " +
                            "issuer y TTL (60-86400 s)"
            );
        }
    }

    public long duracionSegundos() {
        return duracionSegundos;
    }

    public String emitir(Usuario usuario) {

        try {

            Instant ahora = reloj.instant();

            JWTClaimsSet claims = new JWTClaimsSet.Builder()

                    .subject(usuario.getId().toString())

                    .issuer(emisor)

                    .issueTime(Date.from(ahora))

                    .notBeforeTime(Date.from(ahora))

                    .expirationTime(
                            Date.from(
                                    ahora.plusSeconds(duracionSegundos)
                            )
                    )

                    .jwtID(UUID.randomUUID().toString())

                    .claim(
                            "ver",
                            usuario.getTokenVersion()
                    )

                    .build();

            SignedJWT token = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claims
            );

            token.sign(new MACSigner(clave));

            return token.serialize();

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "No se pudo generar el JWT",
                    ex
            );
        }
    }

    public Usuario verificar(String valor) {

        try {

            if (valor == null || valor.isBlank()) {
                throw new IllegalArgumentException(
                        "Token vacío"
                );
            }

            SignedJWT jwt = SignedJWT.parse(valor);

            if (!JWSAlgorithm.HS256.equals(
                    jwt.getHeader().getAlgorithm()
            ) || !jwt.verify(new MACVerifier(clave))) {

                throw new IllegalArgumentException(
                        "Firma incorrecta"
                );
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            Instant ahora = reloj.instant();

            if (!emisor.equals(claims.getIssuer())

                    || claims.getSubject() == null

                    || claims.getExpirationTime() == null

                    || !claims.getExpirationTime()
                    .toInstant()
                    .isAfter(ahora)

                    || claims.getIssueTime() == null

                    || claims.getIssueTime()
                    .toInstant()
                    .isAfter(ahora.plusSeconds(60))

                    || (
                    claims.getNotBeforeTime() != null

                            && claims.getNotBeforeTime()
                            .toInstant()
                            .isAfter(ahora)
            )) {

                throw new IllegalArgumentException(
                        "JWT inválido o expirado"
                );
            }

            Long id = Long.valueOf(
                    claims.getSubject()
            );

            Object version = claims.getClaim("ver");

            if (!(version instanceof Number)) {

                throw new IllegalArgumentException(
                        "Versión ausente"
                );
            }

            Usuario usuario = usuarios.findById(id)

                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Usuario no existe"
                            )
                    );

            if (!usuario.isActivo()

                    || usuario.getTokenVersion()
                    != ((Number) version).longValue()

                    || (
                    !"ADMIN".equals(usuario.getRol())

                            && !"CLIENTE".equals(usuario.getRol())
            )) {

                throw new IllegalArgumentException(
                        "Usuario o sesión inválida"
                );
            }

            return usuario;

        } catch (DataAccessException ex) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Autenticación temporalmente no disponible"
            );

        } catch (Exception ex) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token inválido o expirado"
            );
        }
    }
}