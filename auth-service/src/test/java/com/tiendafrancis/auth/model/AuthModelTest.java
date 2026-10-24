package com.tiendafrancis.auth.model;

import com.tiendafrancis.auth.bean.entity.Usuario;
import com.tiendafrancis.auth.persistence.UsuarioRepository;
import com.tiendafrancis.auth.security.JwtService;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthModelTest {

    private final UsuarioRepository repo =
            mock(UsuarioRepository.class);

    private final JwtService jwt =
            mock(JwtService.class);

    private final AuthModel auth = new AuthModel(
            repo,
            new BCryptPasswordEncoder(4),
            jwt
    );

    @Test
    void registroNormalizaCorreoYAsignaCliente() {

        when(repo.saveAndFlush(any(Usuario.class)))
                .thenAnswer(invocation -> {

                    Usuario usuario = invocation.getArgument(0);

                    ReflectionTestUtils.setField(
                            usuario,
                            "id",
                            7L
                    );

                    assertEquals(
                            "CLIENTE",
                            usuario.getRol()
                    );

                    assertEquals(
                            "cliente@correo.com",
                            usuario.getEmail()
                    );

                    assertNotEquals(
                            "Secreto123",
                            usuario.getPasswordHash()
                    );

                    return usuario;
                });

        Long id = auth.registrar(
                " Cliente@Correo.com ",
                " Cliente ",
                "Secreto123"
        );

        assertEquals(7L, id);
    }

    @Test
    void noPermiteClavesMasLargasDe72Bytes() {

        assertThrows(
                ResponseStatusException.class,

                () -> auth.registrar(
                        "c@a.com",
                        "Prueba",
                        "ñ".repeat(37)
                )
        );

        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void inicioDeSesionInactivoDevuelve401() {

        Usuario usuario = new Usuario(
                "cliente@correo.com",
                "hash",
                "Cliente"
        );

        usuario.cambiarActivo(false);

        when(repo.findByEmail("cliente@correo.com"))
                .thenReturn(Optional.of(usuario));

        var ex = assertThrows(
                ResponseStatusException.class,

                () -> auth.login(
                        "cliente@correo.com",
                        "Secreto123"
                )
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                ex.getStatusCode()
        );
    }

    @Test
    void nuevaContrasenaIncrementaVersion() {

        BCryptPasswordEncoder bcrypt =
                new BCryptPasswordEncoder(4);

        Usuario usuario = new Usuario(
                "cliente@correo.com",
                bcrypt.encode("Anterior123"),
                "Cliente"
        );

        ReflectionTestUtils.setField(
                usuario,
                "id",
                7L
        );

        when(repo.findById(7L))
                .thenReturn(Optional.of(usuario));

        auth.cambiarPassword(
                7L,
                "Anterior123",
                "Nueva12345"
        );

        assertEquals(
                1L,
                usuario.getTokenVersion()
        );

        assertTrue(
                bcrypt.matches(
                        "Nueva12345",
                        usuario.getPasswordHash()
                )
        );
    }
}