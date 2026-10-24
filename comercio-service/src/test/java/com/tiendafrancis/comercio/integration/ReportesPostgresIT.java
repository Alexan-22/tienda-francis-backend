package com.tiendafrancis.comercio.integration;

import com.tiendafrancis.comercio.model.ReportesService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker=true)
@SpringBootTest(properties={"eureka.client.enabled=false","spring.cloud.discovery.enabled=false","spring.jpa.hibernate.ddl-auto=validate"})
class ReportesPostgresIT {
 @Container static final PostgreSQLContainer<?> db=new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("reportes").withUsername("test").withPassword("test");
 @DynamicPropertySource static void config(DynamicPropertyRegistry p){p.add("spring.datasource.url",db::getJdbcUrl);p.add("spring.datasource.username",db::getUsername);p.add("spring.datasource.password",db::getPassword);}
 @Autowired ReportesService reportes;
 @Autowired JdbcTemplate jdbc;
 private Long id(String sql,Object... params){return jdbc.queryForObject(sql,Long.class,params);}
 @BeforeEach void datos(){
  jdbc.execute("TRUNCATE TABLE movimientos_inventario, recepciones_devolucion, devoluciones, pagos, detalle_ventas, ventas, producto_tallas, productos RESTART IDENTITY CASCADE");
  long producto=id("INSERT INTO productos(nombre,precio,activo) VALUES ('Camisa',50,true) RETURNING id");
  long variante=id("INSERT INTO producto_tallas(producto_id,talla,stock_fisico,stock_reservado,version) VALUES (?,'M',3,1,0) RETURNING id",producto);
  long venta=id("INSERT INTO ventas(idempotencia,usuario_id,tipo,estado,pago_estado,total,creada) VALUES (gen_random_uuid(),1,'ONLINE','ENTREGADO','DEVUELTO',100,'2026-10-18T23:30:00Z') RETURNING id");
  long pos=id("INSERT INTO ventas(idempotencia,tipo,estado,pago_estado,total,creada) VALUES (gen_random_uuid(),'PRESENCIAL','ENTREGADO','CONFIRMADO',50,'2026-10-19T10:00:00Z') RETURNING id");
  long detalle=id("INSERT INTO detalle_ventas(venta_id,variante_id,producto_nombre,talla,cantidad,precio_unitario) VALUES (? ,?,'Camisa','M',2,50) RETURNING id",venta,variante);
  long pago=id("INSERT INTO pagos(venta_id,idempotencia,monto,medio,responsable_id,estado,creado) VALUES (?,gen_random_uuid(),100,'YAPE',9,'CONFIRMADO','2026-10-19T02:00:00Z') RETURNING id",venta);
  jdbc.update("INSERT INTO pagos(venta_id,idempotencia,monto,medio,responsable_id,estado,creado) VALUES (?,gen_random_uuid(),50,'EFECTIVO',9,'CONFIRMADO','2026-10-19T14:00:00Z')",pos);
  jdbc.update("INSERT INTO devoluciones(pago_id,idempotencia,monto,motivo,responsable_id,creada) VALUES (?,gen_random_uuid(),30,'Devolucion parcial',9,'2026-10-20T01:00:00Z')",pago);
  long recepcion=id("INSERT INTO recepciones_devolucion(venta_id,detalle_id,idempotencia,cantidad,condicion,motivo,responsable_id,repuesto,recibido_en) VALUES (?,?,gen_random_uuid(),1,'APTO','Retorno',9,true,'2026-10-20T10:00:00Z') RETURNING id",venta,detalle);
  jdbc.update("INSERT INTO movimientos_inventario(variante_id,recepcion_id,cantidad,tipo,motivo,responsable_id,creado_en) VALUES (?,?,1,'REPOSICION_DEVOLUCION','Retorno',9,'2026-10-20T10:00:00Z')",variante,recepcion);
 }
 private ReportesService.Periodo dia(int d){return reportes.periodo(LocalDate.of(2026,10,d),LocalDate.of(2026,10,d));}
 private static BigDecimal amount(Object v){return (BigDecimal)v;}
 @Test void fechaEnLimaIncluyeCobroDelDiaAnteriorUtcYNoReembolsoDeDiaSiguiente(){
  var dia18=reportes.caja(dia(18));
  assertEquals(0,amount(dia18.get("cobradoBruto")).compareTo(new BigDecimal("100.00")));
  assertEquals(0,amount(dia18.get("devuelto")).compareTo(BigDecimal.ZERO));
  var dia19=reportes.caja(dia(19));
  assertEquals(0,amount(dia19.get("cobradoBruto")).compareTo(new BigDecimal("50.00")));
  assertEquals(0,amount(dia19.get("devuelto")).compareTo(new BigDecimal("30.00")));
  assertEquals(0,amount(dia19.get("neto")).compareTo(new BigDecimal("20.00")));
 }
 @Test void inventarioSeparaFisicoReservadoDisponible(){
  var report=reportes.inventario(2);var totales=(Map<?,?>)report.get("totales");
  assertEquals(3L,((Number)totales.get("fisico")).longValue());
  assertEquals(1L,((Number)totales.get("reservado")).longValue());
  assertEquals(2L,((Number)totales.get("disponible")).longValue());
  assertEquals(1,((List<?>)report.get("stockBajo")).size());
 }
 @Test void reportesVentasPagosDevolucionesDistinguenSusFechas(){
  assertEquals(1,((List<?>)reportes.ventas(dia(18)).get("porTipo")).size());
  assertEquals(1,((List<?>)reportes.pagos(dia(19)).get("porMedio")).size());
  assertEquals(0,amount(((Map<?,?>)reportes.devoluciones(dia(18)).get("reembolsos")).get("monto")).compareTo(BigDecimal.ZERO));
  assertEquals(0,amount(((Map<?,?>)reportes.devoluciones(dia(19)).get("reembolsos")).get("monto")).compareTo(new BigDecimal("30.00")));
  assertEquals(1L,((Number)((Map<?,?>)reportes.devoluciones(dia(20)).get("unidadesRepuestas")).get("unidades")).longValue());
 }
 @Test void validaFechasYUmbral(){
  assertThrows(ResponseStatusException.class,()->reportes.periodo(LocalDate.of(2026,10,20),LocalDate.of(2026,10,19)));
  assertThrows(ResponseStatusException.class,()->reportes.periodo(null,LocalDate.of(2026,10,19)));
  assertThrows(ResponseStatusException.class,()->reportes.inventario(-1));
 }
}
