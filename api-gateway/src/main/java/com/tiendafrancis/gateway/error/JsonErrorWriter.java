package com.tiendafrancis.gateway.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

public final class JsonErrorWriter {

    private JsonErrorWriter() {
    }

    public static Mono<Void> responder(
            ServerWebExchange exchange,
            HttpStatus estado
    ) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }

        String tipo = switch (estado.value()) {

            case 401 -> "NO_AUTORIZADO";

            case 403 -> "PROHIBIDO";

            case 404 -> "NO_ENCONTRADO";

            case 503 -> "SERVICIO_NO_DISPONIBLE";

            case 504 -> "TIEMPO_AGOTADO";

            default -> "ERROR";
        };

        String json = "{\"error\":\""
                + tipo
                + "\",\"estado\":"
                + estado.value()
                + "}";

        byte[] bytes = json.getBytes(
                StandardCharsets.UTF_8
        );

        exchange.getResponse().setStatusCode(estado);

        exchange.getResponse()
                .getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

        return exchange.getResponse().writeWith(
                Mono.just(
                        exchange.getResponse()
                                .bufferFactory()
                                .wrap(bytes)
                )
        );
    }
}
