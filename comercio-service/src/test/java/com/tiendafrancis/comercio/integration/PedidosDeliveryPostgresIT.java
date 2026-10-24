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
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@Testcontainers(disabledWithoutDocker=true)
@SpringBootTest(properties={"eureka.client.enabled=false","spring.cloud.discovery.enabled=false","DELIVERY_TARIFFS=LIMA|LIMA|MIRAFLORES=8.50","STOCK_EXPIRATION_SCAN_MS=3600000"})
class PedidosDeliveryPostgresIT {
 @Container static final PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("pedidos_test").withUsername("test").withPassword("test");
 @DynamicPropertySource static void datasource(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
 @Autowired DireccionesService direccionesService;
 @Autowired DireccionRepository direcciones;
 @Autowired ComercioModel comercio;
 @Autowired PedidosService pedidos;
 @Autowired VentaRepository ventas;
 @Autowired VarianteRepository variantes;
 @Autowired ProductoRepository productos;
 @Autowired HistorialRepository historial;
 @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
 @BeforeEach void clean(){jdbc.execute("TRUNCATE TABLE historial_pedidos, direcciones_envio, movimientos_inventario, recepciones_devolucion, devoluciones, pagos, detalle_ventas, ventas, producto_tallas, productos RESTART IDENTITY CASCADE");}
 private DireccionEnvio address(Long user){return direccionesService.crear(user,new DireccionesService.Datos("Destinatario","Av. Principal 123","MIRAFLORES","LIMA","LIMA"));}
 private ProductoTalla stock(){var product=productos.saveAndFlush(new Producto("Camisa",new BigDecimal("50.00")));return variantes.saveAndFlush(new ProductoTalla(product.id,"M",3));}
 private Venta pedido(Long user,Long direccionId,Long variante){return comercio.pedir(new ComercioModel.CrearVenta(UUID.randomUUID(),user,List.of(new ComercioModel.Item(variante,1)),direccionId));}
 @Test void direccionAjenaNoPuedeUtilizarseEnPedido(){var v=stock();var d=address(1L);assertThrows(ResponseStatusException.class,()->pedido(2L,d.id,v.id));}
 @Test void deliveryTotalSnapshotEHistorial(){var v=stock();var d=address(1L);var sale=pedido(1L,d.id,v.id);assertEquals(0,new BigDecimal("58.50").compareTo(sale.total));assertEquals(0,new BigDecimal("8.50").compareTo(sale.deliveryCosto));assertTrue(sale.direccionSnapshot.contains("Av. Principal"));assertEquals(1,historial.findByVentaIdOrderByIdAsc(sale.id).size());}
 @Test void pagoRequeridoYTransicionesAuditadas(){var v=stock();var d=address(1L);var sale=pedido(1L,d.id,v.id);assertThrows(ResponseStatusException.class,()->pedidos.cambiar(sale.id,"EN_PREPARACION",9L,"Preparar"));comercio.registrarPago(sale.id,new ComercioModel.PagoRequest(UUID.randomUUID(),sale.total,"YAPE",null),9L);pedidos.cambiar(sale.id,"EN_PREPARACION",9L,"Preparar");pedidos.cambiar(sale.id,"ENVIADO",9L,"Courier");pedidos.cambiar(sale.id,"ENTREGADO",9L,"Recibido");assertEquals(4,historial.findByVentaIdOrderByIdAsc(sale.id).size());assertThrows(ResponseStatusException.class,()->pedidos.cambiar(sale.id,"RECIBIDO",9L,"Atrás"));}
 @Test void usuarioNoPuedeVerPedidoAjeno(){var v=stock();var d=address(1L);var sale=pedido(1L,d.id,v.id);assertThrows(ResponseStatusException.class,()->pedidos.seguimiento(sale.id,2L,false));assertEquals(1,pedidos.seguimiento(sale.id,1L,false).size());}
 @Test void vencimientoLiberaUnaVezYNoConsumeFisico(){var v=stock();var d=address(1L);var sale=pedido(1L,d.id,v.id);jdbc.update("UPDATE ventas SET vence=now()-interval '2 minutes' WHERE id=?",sale.id);assertEquals(1,pedidos.vencer(20));assertEquals(0,pedidos.vencer(20));var updated=variantes.findById(v.id).orElseThrow();assertEquals(0,updated.stockReservado);assertEquals(3,updated.stockFisico);assertEquals("CANCELADO",ventas.findById(sale.id).orElseThrow().estado);}
 @Test void dosWorkersNoLiberanDosVeces()throws Exception {var v=stock();var d=address(1L);var sale=pedido(1L,d.id,v.id);jdbc.update("UPDATE ventas SET vence=now()-interval '2 minutes' WHERE id=?",sale.id);var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);try{List<Future<Integer>> results=new ArrayList<>();for(int i=0;i<2;i++)results.add(pool.submit(()->{ready.countDown();start.await();return pedidos.vencer(20);}));assertTrue(ready.await(10,TimeUnit.SECONDS));start.countDown();assertEquals(1,results.get(0).get(20,TimeUnit.SECONDS)+results.get(1).get(20,TimeUnit.SECONDS));assertEquals(0,variantes.findById(v.id).orElseThrow().stockReservado);}finally{start.countDown();pool.shutdownNow();}}
}
