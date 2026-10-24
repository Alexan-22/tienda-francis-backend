package com.tiendafrancis.comercio.model;

import com.tiendafrancis.comercio.bean.entity.*;
import com.tiendafrancis.comercio.persistence.*;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PostventaService {

 private final VentaRepository ventas;
 private final DetalleRepository detalles;
 private final VarianteRepository variantes;
 private final PagoRepository pagos;
 private final DevolucionRepository devoluciones;
 private final RecepcionRepository recepciones;
 private final MovimientoRepository movimientos;
 private final IdempotencyLocks idempotencyLocks;
 private final HistorialRepository historial;
 private final Clock clock;

 public record RecibirRequest(
         UUID idempotencia,
         Long detalleId,
         int cantidad,
         String condicion,
         String motivo
 ) {}

 private ResponseStatusException conflicto(String mensaje) {
  return new ResponseStatusException(
          HttpStatus.CONFLICT, mensaje
  );
 }

 private ResponseStatusException invalido(String mensaje) {
  return new ResponseStatusException(
          HttpStatus.BAD_REQUEST, mensaje
  );
 }

 @Transactional
 public Venta cancelar(
         Long id,
         Long actor,
         String motivo
 ) {
  if (actor == null || motivo == null
          || motivo.isBlank() || motivo.length() > 500) {
   throw invalido("Motivo y responsable requeridos");
  }

  Venta venta = ventas.bloquear(id)
          .orElseThrow(() ->
                  new ResponseStatusException(
                          HttpStatus.NOT_FOUND
                  ));

  if ("CANCELADO".equals(venta.estado)) {
   return venta;
  }

  if (!"ONLINE".equals(venta.tipo)
          || !Set.of("RECIBIDO", "EN_PREPARACION")
          .contains(venta.estado)) {
   throw conflicto(
           "Venta ya enviada/entregada o no cancelable"
   );
  }

  BigDecimal cobrado = BigDecimal.ZERO;
  BigDecimal devuelto = BigDecimal.ZERO;

  for (var pago : pagos.findByVentaIdOrderByIdAsc(id)) {
   if ("CONFIRMADO".equals(pago.estado)) {
    cobrado = cobrado.add(pago.monto);

    for (var devolucion :
            devoluciones.findByPagoId(pago.id)) {
     devuelto = devuelto.add(devolucion.monto);
    }
   }
  }

  if (cobrado.compareTo(devuelto) != 0) {
   throw conflicto(
           "Se requiere devolver completamente los cobros"
   );
  }

  if ("PENDIENTE".equals(venta.pagoEstado)) {
   var filas = new ArrayList<>(
           detalles.findByVentaId(id)
   );

   filas.sort(Comparator.comparing(
           d -> d.varianteId
   ));

   for (var fila : filas) {
    var variante = variantes.bloquear(fila.varianteId)
            .orElseThrow(() ->
                    conflicto("Variante desaparecida")
            );

    if (variante.stockReservado < fila.cantidad) {
     throw conflicto("Reserva inconsistente");
    }

    variante.stockReservado -= fila.cantidad;
   }
  } else if (cobrado.signum() > 0
          && devuelto.compareTo(cobrado) == 0) {
   venta.pagoEstado = "DEVUELTO";
  }

  String anterior = venta.estado;
  venta.estado = "CANCELADO";

  historial.save(new HistorialPedido(
          id,
          anterior,
          "CANCELADO",
          actor,
          motivo.trim(),
          clock.instant()
  ));

  return venta;
 }

 @Transactional
 public RecepcionDevolucion recibir(
         Long ventaId,
         RecibirRequest req,
         Long actor
 ) {
  if (actor == null || req == null
          || req.idempotencia() == null
          || req.detalleId() == null
          || req.cantidad() <= 0
          || req.motivo() == null
          || req.motivo().isBlank()
          || req.motivo().length() > 500
          || req.condicion() == null
          || !Set.of("APTO", "DANADO")
          .contains(req.condicion())) {
   throw invalido("Recepción inválida");
  }

  idempotencyLocks.acquire(
          "recepcion", req.idempotencia()
  );

  var venta = ventas.bloquear(ventaId)
          .orElseThrow(() ->
                  new ResponseStatusException(
                          HttpStatus.NOT_FOUND
                  ));

  var anterior = recepciones.findByIdempotencia(
          req.idempotencia()
  );

  if (anterior.isPresent()) {
   var anteriorRecepcion = anterior.get();

   if (!anteriorRecepcion.ventaId.equals(ventaId)
           || !anteriorRecepcion.detalleId.equals(
           req.detalleId())
           || anteriorRecepcion.cantidad != req.cantidad()
           || !anteriorRecepcion.condicion.equals(
           req.condicion())
           || !anteriorRecepcion.motivo.equals(
           req.motivo().trim())
           || !anteriorRecepcion.responsableId.equals(
           actor)) {
    throw conflicto(
            "Clave utilizada para otra recepción"
    );
   }

   return anteriorRecepcion;
  }

  if (!"ENTREGADO".equals(venta.estado)) {
   throw conflicto(
           "Solo puede recibirse mercadería entregada"
   );
  }

  var detalle = detalles.findById(req.detalleId())
          .orElseThrow(() ->
                  new ResponseStatusException(
                          HttpStatus.NOT_FOUND
                  ));

  if (!detalle.ventaId.equals(ventaId)) {
   throw conflicto("Detalle ajeno a la venta");
  }

  int recibidas = recepciones.findByDetalleId(detalle.id)
          .stream()
          .mapToInt(r -> r.cantidad)
          .sum();

  if (req.cantidad() > detalle.cantidad - recibidas) {
   throw conflicto(
           "Cantidad recibida supera cantidad vendida"
   );
  }

  var variante = variantes.bloquear(detalle.varianteId)
          .orElseThrow(() ->
                  conflicto("Variante inexistente")
          );

  boolean apto = "APTO".equals(req.condicion());

  if (apto) {
   variante.stockFisico = Math.addExact(
           variante.stockFisico, req.cantidad()
   );
  }

  var recepcion = recepciones.saveAndFlush(
          new RecepcionDevolucion(
                  ventaId,
                  detalle.id,
                  req.idempotencia(),
                  req.cantidad(),
                  req.condicion(),
                  req.motivo().trim(),
                  actor,
                  apto
          )
  );

  if (apto) {
   movimientos.save(new MovimientoInventario(
           variante.id,
           recepcion.id,
           req.cantidad(),
           "Devolución física: " + req.motivo().trim(),
           actor
   ));
  }

  return recepcion;
 }

 @Transactional(readOnly = true)
 public List<RecepcionDevolucion> recepciones(Long ventaId) {
  if (!ventas.existsById(ventaId)) {
   throw new ResponseStatusException(
           HttpStatus.NOT_FOUND
   );
  }

  return recepciones.findByVentaIdOrderByIdAsc(ventaId);
 }
}