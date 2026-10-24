package com.tiendafrancis.gateway.error;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GatewayExceptionHandler
        implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(
            ServerWebExchange exchange,
            Throwable error
    ) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(error);
        }

        HttpStatus estado = HttpStatus.INTERNAL_SERVER_ERROR;

        if (error instanceof ResponseStatusException ex) {

            estado = HttpStatus.resolve(
                    ex.getStatusCode().value()
            );

            if (estado == null) {
                estado = HttpStatus.INTERNAL_SERVER_ERROR;
            }

        } else if (
                error instanceof java.util.concurrent.TimeoutException

                        || error instanceof io.netty.handler.timeout.ReadTimeoutException
        ) {

            estado = HttpStatus.GATEWAY_TIMEOUT;

        } else if (error instanceof java.net.ConnectException) {

            estado = HttpStatus.SERVICE_UNAVAILABLE;
        }

        return JsonErrorWriter.responder(exchange, estado);
    }
}
