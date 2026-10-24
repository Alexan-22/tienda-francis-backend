package com.tiendafrancis.contenido.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class InternalIntrospectionFilter extends OncePerRequestFilter {

 private final RestClient cliente;
 private final String authUrl;
 private final String claveInterna;

 public InternalIntrospectionFilter(
         @Value("${AUTH_INTERNAL_URL:http://localhost:8081}") String authUrl,
         @Value("${INTERNAL_API_KEY:}") String claveInterna
 ) {
  this.authUrl = authUrl;
  this.claveInterna = claveInterna;

  var factory = new SimpleClientHttpRequestFactory();
  factory.setConnectTimeout(Duration.ofSeconds(3));
  factory.setReadTimeout(Duration.ofSeconds(5));

  cliente = RestClient.builder().requestFactory(factory).build();
 }

 @Override
 protected void doFilterInternal(
         HttpServletRequest request,
         HttpServletResponse response,
         FilterChain chain
 ) throws ServletException, IOException {

  String ruta = request.getRequestURI();

  boolean publico = ruta.equals("/actuator/health")
          || (request.getMethod().equals("GET")
          && ruta.equals("/api/home/banners"));

  if (publico) {
   chain.doFilter(request, response);
   return;
  }

  String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);

  if (bearer == null || !bearer.matches("Bearer [^\\s]+")) {
   response.sendError(401);
   return;
  }

  if (claveInterna.isBlank()) {
   response.sendError(503);
   return;
  }

  Map<?, ?> usuario;

  try {
   usuario = cliente.get()
           .uri(authUrl + "/internal/auth/introspect")
           .header("X-Internal-Key", claveInterna)
           .header(HttpHeaders.AUTHORIZATION, bearer)
           .retrieve()
           .body(Map.class);

  } catch (RestClientResponseException ex) {
   int codigo = ex.getStatusCode().value();
   response.sendError(codigo == 401 || codigo == 403 ? 401 : 503);
   return;

  } catch (Exception ex) {
   response.sendError(503);
   return;
  }

  if (usuario == null || usuario.get("id") == null
          || !Boolean.TRUE.equals(usuario.get("activo"))) {
   response.sendError(401);
   return;
  }

  String rol = String.valueOf(usuario.get("rol"));

  if (!"ADMIN".equals(rol) && !"CLIENTE".equals(rol)) {
   response.sendError(403);
   return;
  }

  var permisos = List.of(new SimpleGrantedAuthority("ROLE_" + rol));

  var autenticacion = new UsernamePasswordAuthenticationToken(
          String.valueOf(usuario.get("id")),
          null,
          permisos
  );

  SecurityContextHolder.getContext().setAuthentication(autenticacion);

  try {
   chain.doFilter(request, response);
  } finally {
   SecurityContextHolder.clearContext();
  }
 }
}