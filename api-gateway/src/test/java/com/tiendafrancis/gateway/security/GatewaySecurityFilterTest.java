package com.tiendafrancis.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GatewaySecurityFilterTest {

    private final AuthIntrospectionClient auth =
            mock(AuthIntrospectionClient.class);

    private final GatewaySecurityFilter filtro =
            new GatewaySecurityFilter(auth);

    @Test
    void compraRechazadaSinBearer() {

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/ventas")
        );

        StepVerifier.create(
                filtro.filter(exchange, e -> Mono.empty())
        ).verifyComplete();

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exchange.getResponse().getStatusCode()
        );

        verifyNoInteractions(auth);
    }

    @Test
    void administradorPuedeRegistrarPago() {

        when(auth.verificar(anyString())).thenReturn(
                Mono.just(
                        new AuthIntrospectionClient.UsuarioActual(
                                4L,
                                "ADMIN",
                                true
                        )
                )
        );

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest
                        .post("/api/ventas/20/pagos")
                        .header("Authorization", "Bearer jwt-valido")
        );

        AtomicBoolean paso = new AtomicBoolean(false);

        WebFilterChain cadena = e -> {
            paso.set(true);
            return Mono.empty();
        };

        StepVerifier.create(
                filtro.filter(exchange, cadena)
        ).verifyComplete();

        assertTrue(paso.get());
    }

    @Test
    void clienteNoPuedeRegistrarPago() {

        when(auth.verificar(anyString())).thenReturn(
                Mono.just(
                        new AuthIntrospectionClient.UsuarioActual(
                                9L,
                                "CLIENTE",
                                true
                        )
                )
        );

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest
                        .post("/api/ventas/20/pagos")
                        .header("Authorization", "Bearer jwt-valido")
        );

        AtomicBoolean paso = new AtomicBoolean(false);

        StepVerifier.create(
                filtro.filter(exchange, e -> {
                    paso.set(true);
                    return Mono.empty();
                })
        ).verifyComplete();

        assertEquals(
                HttpStatus.FORBIDDEN,
                exchange.getResponse().getStatusCode()
        );

        assertFalse(paso.get());
    }

    @Test
    void caidaDeAuthDevuelve503() {

        when(auth.verificar(anyString())).thenReturn(
                Mono.error(
                        new AuthIntrospectionClient.FalloAuth(
                                HttpStatus.SERVICE_UNAVAILABLE
                        )
                )
        );

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest
                        .get("/api/ventas/mis-pedidos")
                        .header("Authorization", "Bearer jwt-valido")
        );

        StepVerifier.create(
                filtro.filter(exchange, e -> Mono.empty())
        ).verifyComplete();

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exchange.getResponse().getStatusCode()
        );
    }
}