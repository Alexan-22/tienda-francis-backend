package com.tiendafrancis.comercio.integration;

import com.tiendafrancis.comercio.bean.entity.*;
import com.tiendafrancis.comercio.model.*;
import com.tiendafrancis.comercio.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Separate service invocations => separate PostgreSQL transactions/connections.
 * Latches synchronize requests without relying on sleeps.
 */
@Testcontainers(disabledWithoutDocker=true)
@SpringBootTest(properties={"eureka.client.enabled=false","spring.cloud.discovery.enabled=false","spring.jpa.hibernate.ddl-auto=validate", "DELIVERY_TARIFFS=LIMA|LIMA|MIRAFLORES=0.00"})
class IdempotenciaConcurrentePostgresIT {
 @Container static final PostgreSQLContainer<?> db=new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("idempotencia").withUsername("test").withPassword("test");
 @DynamicPropertySource static void datasource(DynamicPropertyRegistry r){r.add("spring.datasource.url",db::getJdbcUrl);r.add("spring.datasource.username",db::getUsername);r.add("spring.datasource.password",db::getPassword);}
 @Autowired DireccionRepository direcciones;
 @Autowired ComercioModel comercio; @Autowired PostventaService postventa;
 @Autowired ProductoRepository productos; @Autowired VarianteRepository variantes; @Autowired VentaRepository ventas;
 @Autowired DetalleRepository detalles; @Autowired PagoRepository pagos; @Autowired DevolucionRepository devoluciones;
 @Autowired RecepcionRepository recepciones; @Autowired MovimientoRepository movimientos;
 private Long direccion(Long user){return direcciones.saveAndFlush(new DireccionEnvio(user,"Prueba","Calle 1","MIRAFLORES","LIMA","LIMA")).id;}
 @BeforeEach void clean(){movimientos.deleteAllInBatch();recepciones.deleteAllInBatch();devoluciones.deleteAllInBatch();pagos.deleteAllInBatch();detalles.deleteAllInBatch();ventas.deleteAllInBatch();variantes.deleteAllInBatch();productos.deleteAllInBatch();}
 private ProductoTalla variant(int count){var p=productos.saveAndFlush(new Producto("Polera",new BigDecimal("40.00")));return variantes.saveAndFlush(new ProductoTalla(p.id,"M",count));}
 private <T> List<T> parallel(Callable<T> op) throws Exception {
  var ready=new CountDownLatch(2);var fire=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
  try {
   Callable<T> task=()->{ready.countDown();if(!fire.await(15,TimeUnit.SECONDS))throw new TimeoutException("Barrier timeout");return op.call();};
   var a=executor.submit(task);var b=executor.submit(task);
   assertTrue(ready.await(15,TimeUnit.SECONDS));fire.countDown();
   return List.of(a.get(45,TimeUnit.SECONDS),b.get(45,TimeUnit.SECONDS));
  } finally {executor.shutdownNow();}
 }
 @Test void posMismaClaveMismoActorNoCobraDosVeces() throws Exception {
  var s=variant(3);var key=UUID.randomUUID();
  var req=new ComercioModel.PosRequest(key,"Mostrador",List.of(new ComercioModel.Item(s.id,1)),"EFECTIVO",null);
  var ids=parallel(()->comercio.venderPos(req,9L).id);
  assertEquals(ids.get(0),ids.get(1));assertEquals(1,ventas.count());assertEquals(1,pagos.count());
  assertEquals(2,variantes.findById(s.id).orElseThrow().stockFisico);
 }
 @Test void posMismaClaveOtroContenidoConflicto() throws Exception {
  var s=variant(4);var key=UUID.randomUUID();
  comercio.venderPos(new ComercioModel.PosRequest(key,"Mostrador",List.of(new ComercioModel.Item(s.id,1)),"EFECTIVO",null),9L);
  var conflict=assertThrows(ResponseStatusException.class,()->comercio.venderPos(new ComercioModel.PosRequest(key,"Mostrador",List.of(new ComercioModel.Item(s.id,2)),"EFECTIVO",null),9L));
  assertEquals(409,conflict.getStatusCode().value());assertEquals(3,variantes.findById(s.id).orElseThrow().stockFisico);
 }
 @Test void confirmarMismoPagoConcurrenteConsumeStockUnaVez() throws Exception {
  var s=variant(2);var v=comercio.pedir(new ComercioModel.CrearVenta(UUID.randomUUID(),22L,List.of(new ComercioModel.Item(s.id,1)),direccion(22L)));
  var r=new ComercioModel.PagoRequest(UUID.randomUUID(),v.total,"YAPE","operacion");
  var ids=parallel(()->comercio.registrarPago(v.id,r,9L).id);
  assertEquals(ids.get(0),ids.get(1));assertEquals(1,pagos.count());
  var current=variantes.findById(s.id).orElseThrow();assertEquals(1,current.stockFisico);assertEquals(0,current.stockReservado);
 }
 @Test void mismoReembolsoConcurrenteSeRegistraUnaVez() throws Exception {
  var s=variant(2);var v=comercio.venderPos(new ComercioModel.PosRequest(UUID.randomUUID(),"C",List.of(new ComercioModel.Item(s.id,1)),"EFECTIVO",null),9L);
  var p=pagos.findByVentaIdOrderByIdAsc(v.id).getFirst();
  var r=new ComercioModel.DevolucionRequest(UUID.randomUUID(),new BigDecimal("10.00"),"Cambio");
  var ids=parallel(()->comercio.devolver(p.id,r,9L).id);
  assertEquals(ids.get(0),ids.get(1));assertEquals(1,devoluciones.count());
  assertEquals(1,variantes.findById(s.id).orElseThrow().stockFisico);
 }
 @Test void mismaRecepcionConcurrenteSeReponeSoloUnaVez() throws Exception {
  var s=variant(2);var v=comercio.venderPos(new ComercioModel.PosRequest(UUID.randomUUID(),"C",List.of(new ComercioModel.Item(s.id,1)),"EFECTIVO",null),9L);
  var detail=detalles.findByVentaId(v.id).getFirst();
  var r=new PostventaService.RecibirRequest(UUID.randomUUID(),detail.id,1,"APTO","Devolución aprobada");
  var ids=parallel(()->postventa.recibir(v.id,r,9L).id);
  assertEquals(ids.get(0),ids.get(1));assertEquals(1,recepciones.count());assertEquals(1,movimientos.count());
  assertEquals(2,variantes.findById(s.id).orElseThrow().stockFisico);
 }
 @Test void cancelacionImpagaConcurrenteLiberaReservaUnaVez() throws Exception {
  var s=variant(3);var v=comercio.pedir(new ComercioModel.CrearVenta(UUID.randomUUID(),22L,List.of(new ComercioModel.Item(s.id,2)),direccion(22L)));
  var ids=parallel(()->postventa.cancelar(v.id,9L,"Cancelación").id);
  assertEquals(ids.get(0),ids.get(1));assertEquals("CANCELADO",ventas.findById(v.id).orElseThrow().estado);
  var current=variantes.findById(s.id).orElseThrow();assertEquals(3,current.stockFisico);assertEquals(0,current.stockReservado);
 }
}
