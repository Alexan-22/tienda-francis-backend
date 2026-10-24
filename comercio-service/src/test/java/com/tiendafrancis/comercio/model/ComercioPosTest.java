package com.tiendafrancis.comercio.model;

import com.tiendafrancis.comercio.bean.entity.*;
import com.tiendafrancis.comercio.persistence.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ComercioPosTest {
 @Test void posSinStockNoCreaVentaNiPago(){
  var pr=mock(ProductoRepository.class);var vr=mock(VarianteRepository.class);
  var ventas=mock(VentaRepository.class);var dr=mock(DetalleRepository.class);
  var pagos=mock(PagoRepository.class);var devoluciones=mock(DevolucionRepository.class);
  var service=new ComercioModel(pr,vr,ventas,dr,pagos,devoluciones,mock(IdempotencyLocks.class),mock(DireccionRepository.class),mock(HistorialRepository.class),mock(DeliveryService.class),java.time.Clock.systemUTC());
  var variante=new ProductoTalla(11L,"M",1);variante.id=7L;
  when(ventas.findByIdempotencia(any())).thenReturn(Optional.empty());
  when(vr.bloquear(7L)).thenReturn(Optional.of(variante));
  when(pr.findById(11L)).thenReturn(Optional.of(new Producto("Camisa",new BigDecimal("45.00"))));
  var request=new ComercioModel.PosRequest(UUID.randomUUID(),"Cliente mostrador",List.of(new ComercioModel.Item(7L,2)),"EFECTIVO",null);
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.venderPos(request,99L));
  assertEquals(1,variante.stockFisico);
  verify(ventas,never()).saveAndFlush(any());verify(pagos,never()).save(any());
 }
 @Test void posMismaClaveDistintoActorSeRechaza(){
  var pr=mock(ProductoRepository.class);var vr=mock(VarianteRepository.class);
  var ventas=mock(VentaRepository.class);var dr=mock(DetalleRepository.class);
  var pagos=mock(PagoRepository.class);var devoluciones=mock(DevolucionRepository.class);
  var service=new ComercioModel(pr,vr,ventas,dr,pagos,devoluciones,mock(IdempotencyLocks.class),mock(DireccionRepository.class),mock(HistorialRepository.class),mock(DeliveryService.class),java.time.Clock.systemUTC());
  var key=UUID.randomUUID();var antigua=new Venta(key,null,"PRESENCIAL",new BigDecimal("10.00"),null);antigua.responsableId=99L;
  when(ventas.findByIdempotencia(key)).thenReturn(Optional.of(antigua));
  var request=new ComercioModel.PosRequest(key,"Persona",List.of(new ComercioModel.Item(7L,1)),"EFECTIVO",null);
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.venderPos(request,100L));
 }

 @Test void pagoConMontoInvalidoSeRechazaAntesDeDescontarStock(){
  var pr=mock(ProductoRepository.class);var vr=mock(VarianteRepository.class);
  var sales=mock(VentaRepository.class);var dr=mock(DetalleRepository.class);
  var payments=mock(PagoRepository.class);var refunds=mock(DevolucionRepository.class);
  var svc=new ComercioModel(pr,vr,sales,dr,payments,refunds,mock(IdempotencyLocks.class),mock(DireccionRepository.class),mock(HistorialRepository.class),mock(DeliveryService.class),java.time.Clock.systemUTC());
  var sale=new Venta(UUID.randomUUID(),3L,"ONLINE",new BigDecimal("40.00"),java.time.Instant.now().plusSeconds(900));sale.id=17L;
  when(sales.bloquear(17L)).thenReturn(Optional.of(sale));
  var req=new ComercioModel.PagoRequest(UUID.randomUUID(),new BigDecimal("25.00"),"YAPE",null);
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->svc.registrarPago(17L,req,9L));
  verify(vr,never()).bloquear(any());verify(payments,never()).save(any());
 }
 @Test void devolucionSinMotivoSeRechaza(){
  var svc=new ComercioModel(mock(ProductoRepository.class),mock(VarianteRepository.class),mock(VentaRepository.class),mock(DetalleRepository.class),mock(PagoRepository.class),mock(DevolucionRepository.class),mock(IdempotencyLocks.class),mock(DireccionRepository.class),mock(HistorialRepository.class),mock(DeliveryService.class),java.time.Clock.systemUTC());
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->svc.devolver(1L,new ComercioModel.DevolucionRequest(UUID.randomUUID(),new BigDecimal("1.00")," "),9L));
 }
}
