package com.tiendafrancis.comercio.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class InternalIntrospectionFilter extends OncePerRequestFilter {

 private final RestClient client;
 private final String authUrl;
 private final String internalKey;

 public InternalIntrospectionFilter(
         @Value("${AUTH_INTERNAL_URL:http://localhost:8081}") String authUrl,
         @Value("${INTERNAL_API_KEY:}") String internalKey
 ) {
  this.authUrl = authUrl;
  this.internalKey = internalKey;

  var factory = new SimpleClientHttpRequestFactory();

  factory.setConnectTimeout(Duration.ofSeconds(3));
  factory.setReadTimeout(Duration.ofSeconds(5));

  this.client = RestClient.builder()
          .requestFactory(factory)
          .build();
 }

 @Override
 protected void doFilterInternal(
         HttpServletRequest request,
         HttpServletResponse response,
         FilterChain chain
 ) throws ServletException, IOException {

  String path = request.getRequestURI();

  // Endpoints públicos
  boolean publico =
          path.equals("/actuator/health")
                  || (
                  request.getMethod().equals("GET")
                          && (
                          path.equals("/api/productos")
                                  || path.startsWith("/api/productos/")
                  )
          );

  if (publico) {
   chain.doFilter(request, response);
   return;
  }

  // Obtener token JWT
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

  // Verificar clave interna
  if (internalKey == null || internalKey.isBlank()) {

   response.sendError(
           HttpServletResponse.SC_SERVICE_UNAVAILABLE
   );
   return;
  }

  Map<?, ?> principal;

  // Consultar Auth Service
  try {
   principal = client.get()
           .uri(authUrl + "/internal/auth/introspect")
           .header("X-Internal-Key", internalKey)
           .header(HttpHeaders.AUTHORIZATION, bearer)
           .retrieve()
           .body(Map.class);

  } catch (RestClientResponseException ex) {

   int estado = ex.getStatusCode().value();

   response.sendError(
           estado == 401 || estado == 403 ? 401 : 503
   );
   return;

  } catch (Exception ex) {

   response.sendError(
           HttpServletResponse.SC_SERVICE_UNAVAILABLE
   );
   return;
  }

  // Validar usuario activo
  if (principal == null
          || principal.get("id") == null
          || !Boolean.TRUE.equals(principal.get("activo"))) {

   response.sendError(
           HttpServletResponse.SC_UNAUTHORIZED
   );
   return;
  }

  // Validar rol
  String rol = String.valueOf(
          principal.get("rol")
  );

  if (!"ADMIN".equals(rol) && !"CLIENTE".equals(rol)) {

   response.sendError(
           HttpServletResponse.SC_FORBIDDEN
   );
   return;
  }

  // Registrar autenticación en Spring Security
  var autenticacion = new UsernamePasswordAuthenticationToken(
          String.valueOf(principal.get("id")),
          null,
          List.of(
                  new SimpleGrantedAuthority("ROLE_" + rol)
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