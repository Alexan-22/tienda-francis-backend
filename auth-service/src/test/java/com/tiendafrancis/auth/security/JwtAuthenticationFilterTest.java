package com.tiendafrancis.auth.security;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private final JwtService jwt =
            mock(JwtService.class);

    private final JwtAuthenticationFilter filtro =
            new JwtAuthenticationFilter(jwt);

    @Test
    void bloqueaPerfilSinToken() throws Exception {

        var request = new MockHttpServletRequest(
                "GET",
                "/api/usuarios/me"
        );

        var response = new MockHttpServletResponse();

        var chain = new MockFilterChain();

        filtro.doFilter(request, response, chain);

        assertEquals(
                401,
                response.getStatus()
        );

        assertNull(chain.getRequest());

        verifyNoInteractions(jwt);
    }

    @Test
    void noInterceptaLogin() throws Exception {

        var request = new MockHttpServletRequest(
                "POST",
                "/api/auth/login"
        );

        var response = new MockHttpServletResponse();

        var chain = new MockFilterChain();

        filtro.doFilter(request, response, chain);

        assertEquals(
                200,
                response.getStatus()
        );

        assertNotNull(chain.getRequest());
    }
}
