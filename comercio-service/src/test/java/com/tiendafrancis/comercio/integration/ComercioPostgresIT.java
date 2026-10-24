package com.tiendafrancis.comercio.integration;

import com.tiendafrancis.comercio.bean.entity.*;
import com.tiendafrancis.comercio.model.ComercioModel;
import com.tiendafrancis.comercio.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Tests run against actual PostgreSQL with Flyway and independent service transactions. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties={"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false", "spring.jpa.hibernate.ddl-auto=validate", "DELIVERY_TARIFFS=LIMA|LIMA|MIRAFLORES=0.00"})
@TestMethodOrder(MethodOrderer.DisplayName.class)
class ComercioPostgresIT {
 @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
     .withDatabaseName("tienda_comercio_test").withUsername("test").withPassword("test");
 @DynamicPropertySource static void datasource(DynamicPropertyRegistry r){
  r.add("spring.datasource.url", postgres::getJdbcUrl);
  r.add("spring.datasource.username", postgres::getUsername);
  r.add("spring.datasource.password", postgres::getPassword);
 }
 @Autowired DireccionRepository direcciones;
 @Autowired ComercioModel negocio;
 @Autowired ProductoRepository productos;
 @Autowired VarianteRepository variantes;
 @Autowired VentaRepository ventas;
 @Autowired DetalleRepository detalles;
 @Autowired PagoRepository pagos;
 @Autowired DevolucionRepository devoluciones;
 @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

 private Long direccion(Long user){return direcciones.saveAndFlush(new DireccionEnvio(user,"Prueba","Calle 1","MIRAFLORES","LIMA","LIMA")).id;}
 @BeforeEach void clean(){
  jdbc.execute("TRUNCATE TABLE movimientos_inventario, recepciones_devolucion, devoluciones, pagos, detalle_ventas, ventas, producto_tallas, productos RESTART IDENTITY CASCADE");
 }

 private ProductoTalla stock(int unidades){
  Producto p=productos.saveAndFlush(new Producto("Camisa",new BigDecimal("50.00")));
  return variantes.saveAndFlush(new ProductoTalla(p.id,"M",unidades));
 }
 private ComercioModel.PosRequest pos(UUID key,Long variante,int unidades){
  return new ComercioModel.PosRequest(key,"Comprador mostrador",List.of(new ComercioModel.Item(variante,unidades)),"EFECTIVO",null);
 }
 private Venta posVenta(Long variante,int unidades){return negocio.venderPos(pos(UUID.randomUUID(),variante,unidades),9L);}
 private Venta pedido(Long variante,int unidades){return negocio.pedir(new ComercioModel.CrearVenta(UUID.randomUUID(),21L,List.of(new ComercioModel.Item(variante,unidades)),direccion(21L)));}
 private Pago pagoOnline(Venta venta){return negocio.registrarPago(venta.id,new ComercioModel.PagoRequest(UUID.randomUUID(),venta.total,"YAPE","transferencia"),9L);}

 @Test void posRegistraPagoYDescuentaFisicoSinTocarReserva(){
  var v=stock(3);var venta=posVenta(v.id,2);
  assertEquals("ENTREGADO",venta.estado);assertEquals("CONFIRMADO",venta.pagoEstado);
  var updated=variantes.findById(v.id).orElseThrow();assertEquals(1,updated.stockFisico);assertEquals(0,updated.stockReservado);
  assertEquals(1,detalles.findByVentaId(venta.id).size());
  assertEquals(0,new BigDecimal("100.00").compareTo(pagos.findByVentaIdOrderByIdAsc(venta.id).getFirst().monto));
 }
 @Test void posMismaSolicitudEsIdempotenteYModificacionDaConflicto(){
  var v=stock(4);UUID key=UUID.randomUUID();
  var original=negocio.venderPos(pos(key,v.id,1),9L);
  var repetida=negocio.venderPos(pos(key,v.id,1),9L);
  assertEquals(original.id,repetida.id);assertEquals(3,variantes.findById(v.id).orElseThrow().stockFisico);
  assertEquals(1,pagos.findByVentaIdOrderByIdAsc(original.id).size());
  assertThrows(ResponseStatusException.class,()->negocio.venderPos(pos(key,v.id,2),9L));
 }
 @Test void posInsuficienteNoDejaCambiosParciales(){
  var a=stock(3);var b=stock(1);
  var req=new ComercioModel.PosRequest(UUID.randomUUID(),"C",List.of(new ComercioModel.Item(a.id,1),new ComercioModel.Item(b.id,2)),"EFECTIVO",null);
  assertThrows(ResponseStatusException.class,()->negocio.venderPos(req,9L));
  assertEquals(3,variantes.findById(a.id).orElseThrow().stockFisico);
  assertEquals(1,variantes.findById(b.id).orElseThrow().stockFisico);
  assertEquals(0,ventas.count());assertEquals(0,pagos.count());
 }
 @Test void pagoCompletoConsumeReservaUnaSolaVez(){
  var v=stock(5);var pedido=pedido(v.id,2);
  assertEquals(2,variantes.findById(v.id).orElseThrow().stockReservado);
  var key=UUID.randomUUID();var req=new ComercioModel.PagoRequest(key,pedido.total,"YAPE","referencia");
  var p1=negocio.registrarPago(pedido.id,req,9L);
  var p2=negocio.registrarPago(pedido.id,req,9L);
  assertEquals(p1.id,p2.id);assertEquals(1,pagos.findByVentaIdOrderByIdAsc(pedido.id).size());
  var s=variantes.findById(v.id).orElseThrow();assertEquals(3,s.stockFisico);assertEquals(0,s.stockReservado);
  assertThrows(ResponseStatusException.class,()->negocio.registrarPago(pedido.id,new ComercioModel.PagoRequest(UUID.randomUUID(),pedido.total,"YAPE",null),9L));
 }
 @Test void devolucionesParcialesLimitadasNoReponenStock(){
  var v=stock(3);var venta=posVenta(v.id,1);var pago=pagos.findByVentaIdOrderByIdAsc(venta.id).getFirst();
  var k=UUID.randomUUID();var r=new ComercioModel.DevolucionRequest(k,new BigDecimal("20.00"),"Cambio de opinión");
  var primera=negocio.devolver(pago.id,r,9L);assertEquals(primera.id,negocio.devolver(pago.id,r,9L).id);
  assertThrows(ResponseStatusException.class,()->negocio.devolver(pago.id,new ComercioModel.DevolucionRequest(UUID.randomUUID(),new BigDecimal("31.00"),"Exceso"),9L));
  negocio.devolver(pago.id,new ComercioModel.DevolucionRequest(UUID.randomUUID(),new BigDecimal("30.00"),"Resto"),9L);
  assertEquals("DEVUELTO",ventas.findById(venta.id).orElseThrow().pagoEstado);
  assertEquals(2,variantes.findById(v.id).orElseThrow().stockFisico);
  assertEquals(2,devoluciones.findByPagoId(pago.id).size());
 }
 @Test void devolucionClaveReutilizadaConOtraCantidadSeRechaza(){
  var v=stock(2);var venta=posVenta(v.id,1);var pago=pagos.findByVentaIdOrderByIdAsc(venta.id).getFirst();
  UUID key=UUID.randomUUID();negocio.devolver(pago.id,new ComercioModel.DevolucionRequest(key,new BigDecimal("10.00"),"Motivo"),9L);
  assertThrows(ResponseStatusException.class,()->negocio.devolver(pago.id,new ComercioModel.DevolucionRequest(key,new BigDecimal("20.00"),"Motivo"),9L));
 }
 @Test void dosPosSimultaneosCompitenPorUltimaUnidad() throws Exception {
  var v=stock(1);var start=new CountDownLatch(1);var ready=new CountDownLatch(2);
  ExecutorService pool=Executors.newFixedThreadPool(2);
  try {
   Callable<Boolean> action=()->{ready.countDown();if(!start.await(10,TimeUnit.SECONDS))throw new TimeoutException();
    try{negocio.venderPos(pos(UUID.randomUUID(),v.id,1),9L);return true;}catch(ResponseStatusException ex){assertEquals(409,ex.getStatusCode().value());return false;}};
   var a=pool.submit(action);var b=pool.submit(action);
   assertTrue(ready.await(10,TimeUnit.SECONDS));start.countDown();
   assertNotEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));
   assertEquals(0,variantes.findById(v.id).orElseThrow().stockFisico);
   assertEquals(1,ventas.count());assertEquals(1,pagos.count());
  }finally{start.countDown();pool.shutdownNow();}
 }
 @Test void posYReservaOnlineCompitenPorUltimaUnidad() throws Exception {
  var v=stock(1);var start=new CountDownLatch(1);var ready=new CountDownLatch(2);
  ExecutorService pool=Executors.newFixedThreadPool(2);
  try {
   Callable<Boolean> posTask=()->{ready.countDown();start.await();try{posVenta(v.id,1);return true;}catch(ResponseStatusException e){return false;}};
   Callable<Boolean> onlineTask=()->{ready.countDown();start.await();try{pedido(v.id,1);return true;}catch(ResponseStatusException e){return false;}};
   var a=pool.submit(posTask);var b=pool.submit(onlineTask);
   assertTrue(ready.await(10,TimeUnit.SECONDS));start.countDown();
   assertNotEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));
   var s=variantes.findById(v.id).orElseThrow();assertEquals(0,s.stockFisico-s.stockReservado);
   assertEquals(1,ventas.count());
  }finally{start.countDown();pool.shutdownNow();}
 }
 @Test void onlineDevuelveLaMismaCompraSinDuplicarReservas(){
  var v=stock(5); UUID clave=UUID.randomUUID();
  var direccionFija=direccion(21L);
  var original=negocio.pedir(new ComercioModel.CrearVenta(clave,21L,List.of(new ComercioModel.Item(v.id,2)),direccionFija));
  var repetida=negocio.pedir(new ComercioModel.CrearVenta(clave,21L,List.of(new ComercioModel.Item(v.id,2)),direccionFija));
  assertEquals(original.id,repetida.id);
  assertThrows(ResponseStatusException.class,()->negocio.pedir(new ComercioModel.CrearVenta(clave,21L,List.of(new ComercioModel.Item(v.id,3)),direccionFija)));
  assertEquals(2,variantes.findById(v.id).orElseThrow().stockReservado);
 }

}
