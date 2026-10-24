package com.tiendafrancis.gateway.security;

import io.netty.channel.ChannelOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class AuthIntrospectionClient {

    public record UsuarioActual(
            Long id,
            String rol,
            Boolean activo
    ) {
    }

    public static class FalloAuth extends RuntimeException {

        private final HttpStatus estado;

        public FalloAuth(HttpStatus estado) {
            super(estado.getReasonPhrase());
            this.estado = estado;
        }

        public HttpStatus estado() {
            return estado;
        }
    }

    private final WebClient cliente;
    private final String claveInterna;

    public AuthIntrospectionClient(

            @Value("${gateway.auth.url}")
            String authUrl,

            @Value("${INTERNAL_API_KEY:}")
            String claveInterna

    ) {
        if (claveInterna.getBytes(StandardCharsets.UTF_8).length < 16) {

            throw new IllegalStateException(
                    "Configura INTERNAL_API_KEY (mínimo 16 bytes)"
            );
        }

        this.claveInterna = claveInterna;

        HttpClient http = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                .responseTimeout(Duration.ofSeconds(4));

        this.cliente = WebClient.builder()
                .baseUrl(authUrl)
                .clientConnector(
                        new ReactorClientHttpConnector(http)
                )
                .build();
    }

    public Mono<UsuarioActual> verificar(String bearer) {

        return cliente.get()
                .uri("/internal/auth/introspect")
                .header("X-Internal-Key", claveInterna)
                .header(HttpHeaders.AUTHORIZATION, bearer)

                .exchangeToMono(respuesta -> {

                    int codigo = respuesta.statusCode().value();

                    if (codigo == 401 || codigo == 403) {

                        return Mono.error(
                                new FalloAuth(HttpStatus.UNAUTHORIZED)
                        );
                    }

                    if (codigo != 200) {

                        return Mono.error(
                                new FalloAuth(HttpStatus.SERVICE_UNAVAILABLE)
                        );
                    }

                    return respuesta.bodyToMono(UsuarioActual.class)
                            .switchIfEmpty(
                                    Mono.error(
                                            new FalloAuth(
                                                    HttpStatus.SERVICE_UNAVAILABLE
                                            )
                                    )
                            );
                })

                .timeout(Duration.ofSeconds(5))

                .onErrorMap(
                        error -> !(error instanceof FalloAuth),

                        error -> new FalloAuth(
                                HttpStatus.SERVICE_UNAVAILABLE
                        )
                );
    }
}