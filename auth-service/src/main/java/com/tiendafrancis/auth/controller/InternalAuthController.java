package com.tiendafrancis.auth.controller;

import com.tiendafrancis.auth.bean.dto.AuthDtos.IntrospeccionDto;
import com.tiendafrancis.auth.model.AuthModel;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/internal/auth")
public class InternalAuthController {

    private final AuthModel auth;
    private final byte[] claveInterna;

    public InternalAuthController(

            AuthModel auth,

            @Value("${INTERNAL_API_KEY:}")
            String claveInterna

    ) {

        this.auth = auth;

        this.claveInterna = claveInterna.getBytes(
                StandardCharsets.UTF_8
        );

        if (this.claveInterna.length < 16) {

            throw new IllegalStateException(
                    "Configura INTERNAL_API_KEY con al menos 16 bytes"
            );
        }
    }

    @GetMapping("/introspect")
    public IntrospeccionDto introspect(

            @RequestHeader(
                    value = "X-Internal-Key",
                    required = false
            )
            String key,

            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorization

    ) {

        if (key == null

                || !MessageDigest.isEqual(

                key.getBytes(StandardCharsets.UTF_8),

                claveInterna
        )) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN
            );
        }

        if (authorization == null

                || !authorization.matches(
                "Bearer [^\\s]+"
        )) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED
            );
        }

        var usuario = auth.verificar(
                authorization.substring(7)
        );

        return new IntrospeccionDto(
                usuario.getId(),
                usuario.getRol(),
                usuario.isActivo()
        );
    }
}
