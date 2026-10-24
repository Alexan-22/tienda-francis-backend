package com.tiendafrancis.comercio.model;

import com.tiendafrancis.comercio.bean.entity.*;
import com.tiendafrancis.comercio.persistence.*;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PedidosService {

 private final VentaRepository ventas;
 private final HistorialRepository historial;
 private final DetalleRepository detalles;
 private final VarianteRepository variantes;
 private final Clock clock;

 private ResponseStatusException conflicto(String mensaje) {
  return new ResponseStatusException(
          HttpStatus.CONFLICT, mensaje
  );
 }

 @Transactional
 public Venta cambiar(
         Long id,
         String destino,
         Long admin,
         String motivo
 ) {
  if (admin == null || motivo == null
          || motivo.isBlank() || motivo.length() > 500) {
   throw new ResponseStatusException(
           HttpStatus.BAD_REQUEST
   );
  }

  var venta = ventas.bloquear(id)
          .orElseThrow(() ->
                  new ResponseStatusException(
                          HttpStatus.NOT_FOUND
                  ));

  var permitidos = Map.of(
          "RECIBIDO", "EN_PREPARACION",
          "EN_PREPARACION", "ENVIADO",
          "ENVIADO", "ENTREGADO"
  );

  if (!Objects.equals(
          permitidos.get(venta.estado), destino
  )) {
   throw conflicto("Transición de pedido no permitida");
  }

  if (!"CONFIRMADO".equals(venta.pagoEstado)) {
   throw conflicto("Pago no confirmado");
  }

  String anterior = venta.estado;
  venta.estado = destino;

  historial.save(new HistorialPedido(
          venta.id,
          anterior,
          destino,
          admin,
          motivo.trim(),
          clock.instant()
  ));

  return venta;
 }

 @Transactional(readOnly = true)
 public List<HistorialPedido> seguimiento(
         Long id,
         Long usuario,
         boolean admin
 ) {
  var venta = ventas.findById(id)
          .orElseThrow(() ->
                  new ResponseStatusException(
                          HttpStatus.NOT_FOUND
                  ));

  if (!admin && (
          !"ONLINE".equals(venta.tipo)
                  || !Objects.equals(usuario, venta.usuarioId)
  )) {
   throw new ResponseStatusException(
           HttpStatus.FORBIDDEN
   );
  }

  return historial.findByVentaIdOrderByIdAsc(id);
 }

 @Transactional(readOnly = true)
 public List<Venta> misPedidos(Long usuario) {
  return ventas.findByUsuarioIdAndTipoOrderByIdDesc(
          usuario, "ONLINE"
  );
 }

 @Transactional
 public int vencer(int lote) {
  int vencidas = 0;

  for (Long id : ventas.findExpiradasParaBloquear(
          clock.instant(), lote
  )) {
   Venta venta = ventas.findById(id).orElseThrow();

   if (!"RECIBIDO".equals(venta.estado)
           || !"PENDIENTE".equals(venta.pagoEstado)
           || venta.vence == null
           || venta.vence.isAfter(clock.instant())) {
    continue;
   }

   var filas = new ArrayList<>(
           detalles.findByVentaId(id)
   );

   filas.sort(Comparator.comparing(
           d -> d.varianteId
   ));

   for (var detalle : filas) {
    var stock = variantes.bloquear(detalle.varianteId)
            .orElseThrow(() ->
                    conflicto("Variante no encontrada")
            );

    if (stock.stockReservado < detalle.cantidad) {
     throw conflicto(
             "Stock reservado inconsistente"
     );
    }

    stock.stockReservado -= detalle.cantidad;
   }

   venta.estado = "CANCELADO";

   historial.save(new HistorialPedido(
           id,
           "RECIBIDO",
           "CANCELADO",
           null,
           "Reserva vencida",
           clock.instant()
   ));

   vencidas++;
  }

  return vencidas;
 }
}