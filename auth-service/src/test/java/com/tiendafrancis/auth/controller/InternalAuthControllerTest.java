package com.tiendafrancis.auth.controller;

import com.tiendafrancis.auth.bean.entity.Usuario;
import com.tiendafrancis.auth.model.AuthModel;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InternalAuthControllerTest {

    private final AuthModel auth =
            mock(AuthModel.class);

    private final InternalAuthController controller =
            new InternalAuthController(
                    auth,
                    "ClaveInternaCompartidaDePruebas"
            );

    @Test
    void contratoDeIntrospeccionConservaIdRolYActivo() {

        Usuario usuario = new Usuario(
                "cliente@correo.com",
                "hash",
                "Cliente"
        );

        ReflectionTestUtils.setField(
                usuario,
                "id",
                9L
        );

        when(auth.verificar("jwt-valido"))
                .thenReturn(usuario);

        var dto = controller.introspect(
                "ClaveInternaCompartidaDePruebas",
                "Bearer jwt-valido"
        );

        assertEquals(9L, dto.id());

        assertEquals("CLIENTE", dto.rol());

        assertTrue(dto.activo());
    }

    @Test
    void claveIncorrectaEsRechazadaAntesDeConsultarElToken() {

        var ex = assertThrows(
                ResponseStatusException.class,

                () -> controller.introspect(
                        "incorrecta",
                        "Bearer jwt-valido"
                )
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                ex.getStatusCode()
        );

        verifyNoInteractions(auth);
    }

    @Test
    void noAceptaBearerAusente() {

        var ex = assertThrows(
                ResponseStatusException.class,

                () -> controller.introspect(
                        "ClaveInternaCompartidaDePruebas",
                        null
                )
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                ex.getStatusCode()
        );
    }
}
