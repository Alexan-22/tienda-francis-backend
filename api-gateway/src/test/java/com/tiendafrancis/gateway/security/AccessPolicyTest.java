package com.tiendafrancis.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static com.tiendafrancis.gateway.security.AccessPolicy.Nivel.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AccessPolicyTest {

    @Test
    void publicaSoloLecturaDelCatalogoYRegistro() {

        assertEquals(
                PUBLICO,
                AccessPolicy.resolver(
                        HttpMethod.GET,
                        "/api/productos"
                )
        );

        assertEquals(
                PUBLICO,
                AccessPolicy.resolver(
                        HttpMethod.GET,
                        "/api/home/banners"
                )
        );

        assertEquals(
                PUBLICO,
                AccessPolicy.resolver(
                        HttpMethod.POST,
                        "/api/auth/login"
                )
        );

        assertEquals(
                ADMIN,
                AccessPolicy.resolver(
                        HttpMethod.POST,
                        "/api/productos"
                )
        );
    }

    @Test
    void bloqueaEndpointsInternos() {

        assertEquals(
                DENEGADO,
                AccessPolicy.resolver(
                        HttpMethod.GET,
                        "/internal/auth/introspect"
                )
        );

        assertEquals(
                DENEGADO,
                AccessPolicy.resolver(
                        HttpMethod.GET,
                        "/eureka/apps"
                )
        );

        assertEquals(
                DENEGADO,
                AccessPolicy.resolver(
                        HttpMethod.GET,
                        "/api/auth/debug"
                )
        );
    }

    @Test
    void ventasYAdministracionRespetanRoles() {

        assertEquals(
                AUTENTICADO,
                AccessPolicy.resolver(
                        HttpMethod.POST,
                        "/api/ventas"
                )
        );

        assertEquals(
                AUTENTICADO,
                AccessPolicy.resolver(
                        HttpMethod.GET,
                        "/api/ventas/mis-pedidos"
                )
        );

        assertEquals(
                AUTENTICADO,
                AccessPolicy.resolver(
                        HttpMethod.GET,
                        "/api/ventas/7/seguimiento"
                )
        );

        assertEquals(
                ADMIN,
                AccessPolicy.resolver(
                        HttpMethod.POST,
                        "/api/ventas/pos"
                )
        );

        assertEquals(
                ADMIN,
                AccessPolicy.resolver(
                        HttpMethod.POST,
                        "/api/ventas/7/pagos"
                )
        );

        assertEquals(
                ADMIN,
                AccessPolicy.resolver(
                        HttpMethod.GET,
                        "/api/reportes/ventas"
                )
        );

        assertEquals(
                ADMIN,
                AccessPolicy.resolver(
                        HttpMethod.PATCH,
                        "/api/contenido/banners/2/visibilidad"
                )
        );

        assertEquals(
                DENEGADO,
                AccessPolicy.resolver(
                        HttpMethod.POST,
                        "/api/ventas/extra"
                )
        );
    }
}