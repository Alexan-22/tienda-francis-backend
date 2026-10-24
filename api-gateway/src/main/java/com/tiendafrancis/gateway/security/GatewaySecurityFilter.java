package com.tiendafrancis.gateway.security;

import com.tiendafrancis.gateway.error.JsonErrorWriter;
import com.tiendafrancis.gateway.security.AccessPolicy.Nivel;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class GatewaySecurityFilter implements WebFilter {

    private final AuthIntrospectionClient auth;

    public GatewaySecurityFilter(AuthIntrospectionClient auth) {
        this.auth = auth;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
    ) {
        String ruta = exchange.getRequest()
                .getURI()
                .getRawPath();

        HttpMethod metodo = exchange.getRequest().getMethod();

        // CORS se resuelve en CorsWebFilter

        if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
            return chain.filter(exchange);
        }

        // Rechazar algunas codificaciones ambiguas de ruta

        if (ruta == null
                || ruta.toLowerCase(Locale.ROOT)
                .matches(".*%(2f|5c|2e|25).*")) {

            return JsonErrorWriter.responder(
                    exchange,
                    HttpStatus.BAD_REQUEST
            );
        }

        Nivel nivel = AccessPolicy.resolver(metodo, ruta);

        if (nivel == Nivel.DENEGADO) {

            return JsonErrorWriter.responder(
                    exchange,
                    HttpStatus.NOT_FOUND
            );
        }

        if (nivel == Nivel.PUBLICO) {
            return chain.filter(exchange);
        }

        // Las rutas privadas exigen un único token Bearer

        List<String> cabeceras = exchange.getRequest()
                .getHeaders()
                .getOrEmpty(HttpHeaders.AUTHORIZATION);

        if (cabeceras.size() != 1
                || !cabeceras.get(0).matches("Bearer [^\\s]+")) {

            return JsonErrorWriter.responder(
                    exchange,
                    HttpStatus.UNAUTHORIZED
            );
        }

        return auth.verificar(cabeceras.get(0))

                .flatMap(usuario -> {

                    // Verificar respuesta real de Auth

                    if (usuario.id() == null
                            || usuario.id() < 1

                            || !Boolean.TRUE.equals(
                            usuario.activo()
                    )

                            || (
                            !"ADMIN".equals(usuario.rol())

                                    && !"CLIENTE".equals(usuario.rol())
                    )) {

                        return JsonErrorWriter.responder(
                                exchange,
                                HttpStatus.UNAUTHORIZED
                        );
                    }

                    // Rutas administrativas

                    if (nivel == Nivel.ADMIN
                            && !"ADMIN".equals(usuario.rol())) {

                        return JsonErrorWriter.responder(
                                exchange,
                                HttpStatus.FORBIDDEN
                        );
                    }

                    // Usuario autorizado: continuar al microservicio

                    return chain.filter(exchange);
                })

                .onErrorResume(
                        AuthIntrospectionClient.FalloAuth.class,

                        error -> JsonErrorWriter.responder(
                                exchange,
                                error.estado()
                        )
                );
    }
}