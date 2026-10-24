package com.tiendafrancis.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CorrelationIdFilter implements WebFilter {

    public static final String HEADER = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
    ) {
        String recibido = exchange.getRequest()
                .getHeaders().getFirst(HEADER);

        String id = esUuid(recibido)
                ? recibido
                : UUID.randomUUID().toString();

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> {

                    headers.remove(HEADER);
                    headers.set(HEADER, id);

                    headers.remove("X-Internal-Key");
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Role");
                    headers.remove("X-User-Email");
                    headers.remove("X-Authenticated-User");

                    headers.remove("Forwarded");
                    headers.remove("X-Forwarded-For");
                    headers.remove("X-Forwarded-Host");
                    headers.remove("X-Forwarded-Proto");
                    headers.remove("X-Forwarded-Port");
                })
                .build();

        exchange.getResponse()
                .getHeaders()
                .set(HEADER, id);

        return chain.filter(
                exchange.mutate().request(request).build()
        );
    }

    private boolean esUuid(String valor) {

        if (valor == null || valor.length() != 36) {
            return false;
        }

        try {
            return UUID.fromString(valor)
                    .toString()
                    .equalsIgnoreCase(valor);

        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}