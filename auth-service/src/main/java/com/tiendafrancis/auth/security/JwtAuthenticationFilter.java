package com.tiendafrancis.auth.security;

import com.tiendafrancis.auth.bean.entity.Usuario;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        String path = request.getRequestURI();

        return !path.equals("/api/usuarios")
                && !path.startsWith("/api/usuarios/");
    }

    @Override
    protected void doFilterInternal(

            HttpServletRequest request,

            HttpServletResponse response,

            FilterChain chain

    ) throws ServletException, IOException {

        String bearer = request.getHeader(
                HttpHeaders.AUTHORIZATION
        );

        if (bearer == null
                || !bearer.matches("Bearer [^\\s]+")) {

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            return;
        }

        Usuario usuario;

        try {

            usuario = jwtService.verificar(
                    bearer.substring(7)
            );

        } catch (ResponseStatusException ex) {

            response.sendError(
                    ex.getStatusCode().value() == 503

                            ? HttpServletResponse.SC_SERVICE_UNAVAILABLE

                            : HttpServletResponse.SC_UNAUTHORIZED
            );

            return;
        }

        var autenticacion =
                new UsernamePasswordAuthenticationToken(

                        usuario.getId().toString(),

                        null,

                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + usuario.getRol()
                                )
                        )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(autenticacion);

        try {

            chain.doFilter(request, response);

        } finally {

            SecurityContextHolder.clearContext();
        }
    }
}
