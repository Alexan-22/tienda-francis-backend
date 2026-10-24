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
@Testcontainers(disabledWithoutDocker=true)
@SpringBootTest(properties={"eureka.client.enabled=false","spring.cloud.discovery.enabled=false","spring.jpa.hibernate.ddl-auto=validate", "DELIVERY_TARIFFS=LIMA|LIMA|MIRAFLORES=0.00"})
class PostventaPostgresIT {
 @Container static final PostgreSQLContainer<?> db=new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("postventa").withUsername("test").withPassword("test");
 @DynamicPropertySource static void cfg(DynamicPropertyRegistry r){r.add("spring.datasource.url",db::getJdbcUrl);r.add("spring.datasource.username",db::getUsername);r.add("spring.datasource.password",db::getPassword);}
 @Autowired DireccionRepository direcciones;
 @Autowired ComercioModel comercio; @Autowired PostventaService postventa;
 @Autowired ProductoRepository productos; @Autowired VarianteRepository variantes;
 @Autowired VentaRepository ventas; @Autowired DetalleRepository detalles; @Autowired PagoRepository pagos;
 @Autowired DevolucionRepository devoluciones; @Autowired RecepcionRepository recepciones; @Autowired MovimientoRepository movimientos;
 private Long direccion(Long user){return direcciones.saveAndFlush(new DireccionEnvio(user,"Prueba","Calle 1","MIRAFLORES","LIMA","LIMA")).id;}
 @BeforeEach void limpiar(){movimientos.deleteAllInBatch();recepciones.deleteAllInBatch();devoluciones.deleteAllInBatch();pagos.deleteAllInBatch();detalles.deleteAllInBatch();ventas.deleteAllInBatch();variantes.deleteAllInBatch();productos.deleteAllInBatch();}
 private ProductoTalla stock(int n){var p=productos.saveAndFlush(new Producto("Camisa",new BigDecimal("50.00")));return variantes.saveAndFlush(new ProductoTalla(p.id,"M",n));}
 private Venta pedido(Long variante,int n){return comercio.pedir(new ComercioModel.CrearVenta(UUID.randomUUID(),25L,List.of(new ComercioModel.Item(variante,n)),direccion(25L)));}
 private Venta pos(Long variante,int n){return comercio.venderPos(new ComercioModel.PosRequest(UUID.randomUUID(),"Mostrador",List.of(new ComercioModel.Item(variante,n)),"EFECTIVO",null),9L);}
 private PostventaService.RecibirRequest recibe(Long detalle,int n,String condicion,UUID key){return new PostventaService.RecibirRequest(key,detalle,n,condicion,"Revisión de prenda");}
 @Test void cancelarImpagoLiberaReservaUnaVez(){var s=stock(3);var v=pedido(s.id,2);postventa.cancelar(v.id,9L,"Solicitud cliente");postventa.cancelar(v.id,9L,"Solicitud cliente");var result=variantes.findById(s.id).orElseThrow();assertEquals(3,result.stockFisico);assertEquals(0,result.stockReservado);assertEquals("CANCELADO",ventas.findById(v.id).orElseThrow().estado);}
 @Test void pedidoPagadoRequiereReembolsoCompletoYNoReponeFisico(){var s=stock(2);var v=pedido(s.id,1);var p=comercio.registrarPago(v.id,new ComercioModel.PagoRequest(UUID.randomUUID(),v.total,"YAPE",null),9L);assertThrows(ResponseStatusException.class,()->postventa.cancelar(v.id,9L,"Anular"));comercio.devolver(p.id,new ComercioModel.DevolucionRequest(UUID.randomUUID(),v.total,"Anular"),9L);postventa.cancelar(v.id,9L,"Anular");assertEquals("DEVUELTO",ventas.findById(v.id).orElseThrow().pagoEstado);assertEquals(1,variantes.findById(s.id).orElseThrow().stockFisico);}
 @Test void entregarPosRecibirAptoReponeAuditaYSigueIdempotencia(){var s=stock(3);var v=pos(s.id,2);var row=detalles.findByVentaId(v.id).getFirst();var key=UUID.randomUUID();var one=postventa.recibir(v.id,recibe(row.id,1,"APTO",key),9L);assertEquals(one.id,postventa.recibir(v.id,recibe(row.id,1,"APTO",key),9L).id);assertEquals(2,variantes.findById(s.id).orElseThrow().stockFisico);assertEquals(1,movimientos.findByRecepcionId(one.id).size());assertEquals(1,recepciones.findByDetalleId(row.id).size());}
 @Test void prendaDanadaNoReponeInventario(){var s=stock(2);var v=pos(s.id,1);var row=detalles.findByVentaId(v.id).getFirst();var receipt=postventa.recibir(v.id,recibe(row.id,1,"DANADO",UUID.randomUUID()),9L);assertFalse(receipt.repuesto);assertEquals(1,variantes.findById(s.id).orElseThrow().stockFisico);assertTrue(movimientos.findByRecepcionId(receipt.id).isEmpty());}
 @Test void impideRecibirMasUnidadesQueVendidasYReutilizarClave(){var s=stock(2);var v=pos(s.id,1);var row=detalles.findByVentaId(v.id).getFirst();var key=UUID.randomUUID();postventa.recibir(v.id,recibe(row.id,1,"APTO",key),9L);assertThrows(ResponseStatusException.class,()->postventa.recibir(v.id,recibe(row.id,1,"DANADO",key),9L));assertThrows(ResponseStatusException.class,()->postventa.recibir(v.id,recibe(row.id,1,"APTO",UUID.randomUUID()),9L));}
 @Test void impideRecepcionesDePedidosNoEntregados(){var s=stock(2);var v=pedido(s.id,1);var row=detalles.findByVentaId(v.id).getFirst();assertThrows(ResponseStatusException.class,()->postventa.recibir(v.id,recibe(row.id,1,"APTO",UUID.randomUUID()),9L));}
 @Test void dosRecepcionesSimultaneasPorMismaUnidadSoloUnaProcede() throws Exception {var s=stock(2);var v=pos(s.id,1);var detail=detalles.findByVentaId(v.id).getFirst();var ready=new CountDownLatch(2);var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);try {Callable<Boolean> task=()->{ready.countDown();if(!start.await(10,TimeUnit.SECONDS))throw new TimeoutException();try {postventa.recibir(v.id,recibe(detail.id,1,"APTO",UUID.randomUUID()),9L);return true;}catch(ResponseStatusException ex){assertEquals(409,ex.getStatusCode().value());return false;}};var a=pool.submit(task);var b=pool.submit(task);assertTrue(ready.await(10,TimeUnit.SECONDS));start.countDown();assertNotEquals(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS));assertEquals(2,variantes.findById(s.id).orElseThrow().stockFisico);assertEquals(1,recepciones.findByDetalleId(detail.id).size());assertEquals(1,movimientos.count());}finally{pool.shutdownNow();}}
}
