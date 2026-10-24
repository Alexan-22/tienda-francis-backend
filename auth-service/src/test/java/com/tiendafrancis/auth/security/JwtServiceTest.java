package com.tiendafrancis.auth.security;

import com.tiendafrancis.auth.bean.entity.Usuario;
import com.tiendafrancis.auth.persistence.UsuarioRepository;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtServiceTest {

    private final UsuarioRepository repo =
            mock(UsuarioRepository.class);

    private final Clock reloj = Clock.fixed(
            Instant.parse("2026-10-23T12:00:00Z"),
            ZoneOffset.UTC
    );

    private final JwtService jwt = new JwtService(

            repo,

            reloj,

            "ClaveSeguraDePruebas0123456789ABCDEF123456",

            "tienda-francis",

            3600
    );

    @Test
    void firmaYVerificaUsuarioActivo() {

        Usuario usuario = new Usuario(
                "cliente@correo.com",
                "hash",
                "Cliente"
        );

        ReflectionTestUtils.setField(
                usuario,
                "id",
                5L
        );

        when(repo.findById(5L))
                .thenReturn(Optional.of(usuario));

        assertSame(
                usuario,
                jwt.verificar(jwt.emitir(usuario))
        );
    }

    @Test
    void unCambioDeContrasenaInvalidaElTokenAnterior() {

        Usuario usuario = new Usuario(
                "cliente@correo.com",
                "hash",
                "Cliente"
        );

        ReflectionTestUtils.setField(
                usuario,
                "id",
                5L
        );

        when(repo.findById(5L))
                .thenReturn(Optional.of(usuario));

        String tokenAnterior = jwt.emitir(usuario);

        usuario.cambiarPassword("nuevo-hash-bcrypt");

        var ex = assertThrows(
                ResponseStatusException.class,

                () -> jwt.verificar(tokenAnterior)
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                ex.getStatusCode()
        );
    }
}
