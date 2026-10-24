package com.tiendafrancis.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 20)
    public CorsWebFilter corsWebFilter(
            @Value("${gateway.cors.origins}") String origins
    ) {
        List<String> permitidos = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        if (permitidos.isEmpty() || permitidos.contains("*")) {
            throw new IllegalStateException(
                    "Configura orígenes CORS explícitos"
            );
        }

        CorsConfiguration cors = new CorsConfiguration();

        cors.setAllowedOrigins(permitidos);

        cors.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        cors.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Idempotency-Key",
                "X-Correlation-ID"
        ));

        cors.setExposedHeaders(List.of(
                "Location",
                "X-Correlation-ID"
        ));

        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", cors);

        return new CorsWebFilter(source);
    }
}