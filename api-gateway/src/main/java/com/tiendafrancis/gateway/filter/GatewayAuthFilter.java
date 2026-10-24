package com.tiendafrancis.gateway.filter;
@org.springframework.stereotype.Component
public class GatewayAuthFilter implements org.springframework.web.server.WebFilter {
 private final org.springframework.web.reactive.function.client.WebClient client;
 public GatewayAuthFilter(org.springframework.web.reactive.function.client.WebClient.Builder builder) {this.client=builder.build();}
 private boolean publicRequest(org.springframework.http.server.reactive.ServerHttpRequest r) {
 String p=r.getPath().value();return p.equals("/actuator/health")||p.equals("/api/auth/login")||p.equals("/api/auth/registro")||(r.getMethod()==org.springframework.http.HttpMethod.GET&&(p.startsWith("/api/productos")||p.startsWith("/api/categorias")||p.startsWith("/api/generos")||p.startsWith("/api/tallas")||p.startsWith("/api/home")));
 }
 @org.springframework.beans.factory.annotation.Value("${AUTH_INTERNAL_URL:http://localhost:8081}") String authUrl;
 @org.springframework.beans.factory.annotation.Value("${INTERNAL_API_KEY:}") String apiKey;
 public reactor.core.publisher.Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange, org.springframework.web.server.WebFilterChain chain) {
 if(exchange.getRequest().getPath().value().startsWith("/internal/")) {
  exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.NOT_FOUND);
  return exchange.getResponse().setComplete();
 }
 if(publicRequest(exchange.getRequest())) return chain.filter(exchange);
 if(apiKey==null||apiKey.isBlank()) {
  exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
  return exchange.getResponse().setComplete();
 }
 String bearer=exchange.getRequest().getHeaders().getFirst(org.springframework.http.HttpHeaders.AUTHORIZATION);
 if(bearer==null||!bearer.startsWith("Bearer ")) {exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);return exchange.getResponse().setComplete();}
 return client.get().uri(authUrl+"/internal/auth/introspect").header(org.springframework.http.HttpHeaders.AUTHORIZATION,bearer).header("X-Internal-Key",apiKey).retrieve().toEntity(String.class).flatMap(resp->{ if(!resp.getStatusCode().is2xxSuccessful()){exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);return exchange.getResponse().setComplete();}return chain.filter(exchange);}).onErrorResume(org.springframework.web.reactive.function.client.WebClientResponseException.class,ex->{exchange.getResponse().setStatusCode(ex.getStatusCode().value()==401?org.springframework.http.HttpStatus.UNAUTHORIZED:ex.getStatusCode().value()==403?org.springframework.http.HttpStatus.FORBIDDEN:org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);return exchange.getResponse().setComplete();}).onErrorResume(ex->{exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);return exchange.getResponse().setComplete();});
 }
}
