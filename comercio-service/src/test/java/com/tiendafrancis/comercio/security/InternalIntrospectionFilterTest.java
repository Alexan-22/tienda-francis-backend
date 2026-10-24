package com.tiendafrancis.comercio.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class InternalIntrospectionFilterTest {
 private final InternalIntrospectionFilter filter = new InternalIntrospectionFilter("http://localhost:8081", "");
 @Test void operacionesPrivadasSinBearerDevuelven401() throws Exception {
  for(String path:new String[]{"/api/ventas/pos","/api/ventas/1/pagos","/api/pagos/1/devoluciones","/api/ventas/1/cancelar","/api/ventas/1/recepciones"}){
   var req=new MockHttpServletRequest("POST",path);var resp=new MockHttpServletResponse();var chain=new MockFilterChain();
   filter.doFilter(req,resp,chain);
   assertEquals(401,resp.getStatus(),path);assertNull(chain.getRequest(),path);
  }
 }
 @Test void credencialInternaAusenteEs503NoPermisoPorDefecto() throws Exception {
  var req=new MockHttpServletRequest("POST","/api/ventas/pos");req.addHeader("Authorization","Bearer token-de-prueba");
  var response=new MockHttpServletResponse();var chain=new MockFilterChain();
  filter.doFilter(req,response,chain);
  assertEquals(503,response.getStatus());assertNull(chain.getRequest());
 }
 @Test void catalogoPublicoNoNecesitaToken() throws Exception {
  var req=new MockHttpServletRequest("GET","/api/productos");var response=new MockHttpServletResponse();var chain=new MockFilterChain();
  filter.doFilter(req,response,chain);
  assertEquals(200,response.getStatus());assertNotNull(chain.getRequest());
 }
 @Test void escrituraProductosNoEsPublica() throws Exception {
  var req=new MockHttpServletRequest("POST","/api/productos");var response=new MockHttpServletResponse();
  filter.doFilter(req,response,new MockFilterChain());
  assertEquals(401,response.getStatus());
 }
}

