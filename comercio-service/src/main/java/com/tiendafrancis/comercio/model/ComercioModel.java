package com.tiendafrancis.comercio.model;

import com.tiendafrancis.comercio.bean.entity.*;
import com.tiendafrancis.comercio.persistence.*;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ComercioModel {

 private final ProductoRepository productos;
 private final VarianteRepository variantes;
 private final VentaRepository ventas;
 private final DetalleRepository detalles;
 private final PagoRepository pagos;
 private final DevolucionRepository devoluciones;
 private final IdempotencyLocks idempotencyLocks;
 private final DireccionRepository direcciones;
 private final HistorialRepository historial;
 private final DeliveryService delivery;
 private final Clock clock;

 @Value("${STOCK_RESERVATION_TTL_SECONDS:900}")
 private long ttl;

 public record Item(
         Long varianteId,
         int cantidad
 ) {}

 public record CrearVenta(
         UUID idempotencia,
         Long usuarioId,
         List<Item> items,
         Long direccionId
 ) {
  public CrearVenta(
          UUID idempotencia,
          Long usuarioId,
          List<Item> items
  ) {
   this(idempotencia, usuarioId, items, null);
  }
 }

 public record PosRequest(
         UUID idempotencia,
         String clienteNombre,
         List<Item> items,
         String medio,
         String referencia
 ) {}

 public record PagoRequest(
         UUID idempotencia,
         BigDecimal monto,
         String medio,
         String referencia
 ) {}

 public record DevolucionRequest(
         UUID idempotencia,
         BigDecimal monto,
         String motivo
 ) {}

 private ResponseStatusException invalido(String mensaje) {
  return new ResponseStatusException(
          HttpStatus.BAD_REQUEST, mensaje
  );
 }

 private ResponseStatusException conflicto(String mensaje) {
  return new ResponseStatusException(
          HttpStatus.CONFLICT, mensaje
  );
 }

 private BigDecimal dinero(BigDecimal monto) {
  if (monto == null || monto.signum() <= 0
          || monto.scale() > 2) {
   throw invalido(
           "Monto positivo con hasta dos decimales requerido"
   );
  }

  return monto.setScale(2);
 }

 private Map<Long, Integer> agrupar(List<Item> items) {
  if (items == null || items.isEmpty()) {
   throw invalido("Se requieren productos");
  }

  Map<Long, Integer> resultado = new TreeMap<>();

  try {
   for (var item : items) {
    if (item == null || item.varianteId() == null
            || item.cantidad() <= 0) {
     throw invalido(
             "Variante y cantidad inválidas"
     );
    }

    resultado.merge(
            item.varianteId(),
            item.cantidad(),
            Math::addExact
    );
   }
  } catch (ArithmeticException ex) {
   throw invalido("Cantidad fuera de rango");
  }

  return resultado;
 }

 private String hash(String contenido) {
  try {
   var digest = MessageDigest.getInstance("SHA-256");

   return HexFormat.of().formatHex(
           digest.digest(
                   contenido.getBytes(
                           StandardCharsets.UTF_8
                   )
           )
   );
  } catch (NoSuchAlgorithmException ex) {
   throw new IllegalStateException(ex);
  }
 }

 private String huellaOnline(
         Long usuario,
         Map<Long, Integer> items,
         Long direccionId
 ) {
  return hash(
          usuario + "|ONLINE|" + direccionId + "|" + items
  );
 }

 private String huellaPos(
         PosRequest req,
         Map<Long, Integer> items
 ) {
  return hash(
          String.valueOf(
                  req.clienteNombre() == null
                          ? ""
                          : req.clienteNombre().trim()
          )
                  + "|" + req.medio()
                  + "|" + String.valueOf(req.referencia())
                  + "|" + items
  );
 }

 private void validarMedio(String medio, boolean pos) {
  if (medio == null
          || !Set.of("YAPE", "TARJETA", "EFECTIVO")
          .contains(medio)
          || (!pos && "EFECTIVO".equals(medio))) {
   throw invalido("Medio de pago inválido");
  }
 }

 // Productos

 @Transactional
 public Producto crearProducto(
         String nombre,
         BigDecimal precio
 ) {
  if (precio == null || precio.signum() <= 0) {
   throw invalido("Precio inválido");
  }

  return productos.save(
          new Producto(
                  nombre,
                  precio.setScale(2, RoundingMode.HALF_UP)
          )
  );
 }

 public List<Producto> catalogo() {
  return productos.findByActivoTrue();
 }

 @Transactional
 public ProductoTalla crearVariante(
         Long productoId,
         String talla,
         int stock
 ) {
  if (stock < 0 || talla == null || talla.isBlank()) {
   throw invalido("Talla o stock inválido");
  }

  if (!productos.existsById(productoId)) {
   throw new ResponseStatusException(
           HttpStatus.NOT_FOUND
   );
  }

  return variantes.save(
          new ProductoTalla(productoId, talla, stock)
  );
 }

 // Compra online

 @Transactional
 public Venta pedir(CrearVenta req) {
  if (req == null || req.idempotencia() == null
          || req.usuarioId() == null
          || req.items() == null
          || req.items().isEmpty()
          || req.direccionId() == null) {
   throw invalido("Datos de compra incompletos");
  }

  var direccion = direcciones
          .findByIdAndUsuarioIdAndActivaTrue(
                  req.direccionId(),
                  req.usuarioId()
          )
          .orElseThrow(() ->
                  invalido("Dirección inválida o ajena")
          );

  BigDecimal costoDelivery = delivery.costo(direccion);

  idempotencyLocks.acquire(
          "venta", req.idempotencia()
  );

  Map<Long, Integer> items = agrupar(req.items());

  String huella = huellaOnline(
          req.usuarioId(), items, req.direccionId()
  );

  var anterior = ventas.findByIdempotencia(
          req.idempotencia()
  );

  if (anterior.isPresent()) {
   var venta = anterior.get();

   if ("ONLINE".equals(venta.tipo)
           && req.usuarioId().equals(venta.usuarioId)
           && huella.equals(venta.solicitudHash)) {
    return venta;
   }

   throw conflicto(
           "Clave de compra ya utilizada con datos diferentes"
   );
  }

  BigDecimal subtotal = BigDecimal.ZERO;
  List<Object[]> snapshots = new ArrayList<>();

  for (var item : items.entrySet()) {
   var variante = variantes.bloquear(item.getKey())
           .orElseThrow(() ->
                   new ResponseStatusException(
                           HttpStatus.NOT_FOUND
                   ));

   var producto = productos.findById(variante.productoId)
           .orElseThrow();

   if (!producto.activo) {
    throw conflicto("Producto inactivo");
   }

   if (variante.stockFisico - variante.stockReservado
           < item.getValue()) {
    throw conflicto("Stock insuficiente");
   }

   variante.stockReservado += item.getValue();

   BigDecimal precio = producto.precioOferta == null
           ? producto.precio
           : producto.precioOferta;

   subtotal = subtotal.add(
           precio.multiply(
                   BigDecimal.valueOf(item.getValue())
           )
   );

   snapshots.add(new Object[]{
           variante.id,
           producto.nombre,
           variante.talla,
           item.getValue(),
           precio
   });
  }

  Venta venta = new Venta(
          req.idempotencia(),
          req.usuarioId(),
          "ONLINE",
          subtotal.add(costoDelivery)
                  .setScale(2, RoundingMode.HALF_UP),
          clock.instant().plusSeconds(ttl)
  );

  venta.subtotal = subtotal.setScale(
          2, RoundingMode.HALF_UP
  );

  venta.deliveryCosto = costoDelivery;
  venta.direccionId = direccion.id;

  venta.direccionSnapshot =
          direccion.destinatario + " | "
                  + direccion.linea + " | "
                  + direccion.distrito + " | "
                  + direccion.provincia + " | "
                  + direccion.departamento;

  venta.solicitudHash = huella;
  venta = ventas.save(venta);

  for (Object[] item : snapshots) {
   detalles.save(new DetalleVenta(
           venta.id,
           (Long) item[0],
           (String) item[1],
           (String) item[2],
           (Integer) item[3],
           (BigDecimal) item[4]
   ));
  }

  historial.save(new HistorialPedido(
          venta.id,
          "CREADO",
          "RECIBIDO",
          req.usuarioId(),
          "Pedido creado y stock reservado",
          clock.instant()
  ));

  return venta;
 }

 // Punto de venta presencial

 @Transactional
 public Venta venderPos(
         PosRequest req,
         Long adminId
 ) {
  if (req == null || req.idempotencia() == null
          || adminId == null
          || req.clienteNombre() == null
          || req.clienteNombre().isBlank()
          || req.clienteNombre().length() > 255) {
   throw invalido("Datos POS incompletos");
  }

  validarMedio(req.medio(), true);

  idempotencyLocks.acquire(
          "venta", req.idempotencia()
  );

  var items = agrupar(req.items());
  String huella = huellaPos(req, items);

  var anterior = ventas.findByIdempotencia(
          req.idempotencia()
  );

  if (anterior.isPresent()) {
   var venta = anterior.get();

   if (!"PRESENCIAL".equals(venta.tipo)
           || !adminId.equals(venta.responsableId)
           || !huella.equals(venta.solicitudHash)) {
    throw conflicto(
            "Clave de idempotencia usada con otra solicitud"
    );
   }

   return venta;
  }

  BigDecimal total = BigDecimal.ZERO;
  List<Object[]> snapshots = new ArrayList<>();

  for (var item : items.entrySet()) {
   var variante = variantes.bloquear(item.getKey())
           .orElseThrow(() ->
                   new ResponseStatusException(
                           HttpStatus.NOT_FOUND,
                           "Variante inexistente"
                   ));

   var producto = productos.findById(variante.productoId)
           .orElseThrow();

   if (!producto.activo) {
    throw conflicto("Producto inactivo");
   }

   if (variante.stockFisico - variante.stockReservado
           < item.getValue()) {
    throw conflicto("Stock insuficiente");
   }

   BigDecimal precio = producto.precioOferta == null
           ? producto.precio
           : producto.precioOferta;

   variante.stockFisico -= item.getValue();

   total = total.add(
           precio.multiply(
                   BigDecimal.valueOf(item.getValue())
           )
   );

   snapshots.add(new Object[]{
           variante.id,
           producto.nombre,
           variante.talla,
           item.getValue(),
           precio
   });
  }

  Venta venta = new Venta(
          req.idempotencia(),
          null,
          "PRESENCIAL",
          total.setScale(2, RoundingMode.HALF_UP),
          null
  );

  venta.responsableId = adminId;
  venta.clientePresencialNombre = req.clienteNombre().trim();
  venta.solicitudHash = huella;
  venta.estado = "ENTREGADO";
  venta.pagoEstado = "CONFIRMADO";

  venta = ventas.saveAndFlush(venta);

  for (Object[] item : snapshots) {
   detalles.save(new DetalleVenta(
           venta.id,
           (Long) item[0],
           (String) item[1],
           (String) item[2],
           (Integer) item[3],
           (BigDecimal) item[4]
   ));
  }

  UUID pagoId = UUID.nameUUIDFromBytes(
          ("POS-PAGO:" + req.idempotencia())
                  .getBytes(StandardCharsets.UTF_8)
  );

  pagos.save(new Pago(
          venta.id,
          pagoId,
          total,
          req.medio(),
          req.referencia(),
          adminId
  ));

  return venta;
 }

 // Registro de pagos

 @Transactional
 public Pago registrarPago(
         Long ventaId,
         PagoRequest req,
         Long adminId
 ) {
  if (req == null || req.idempotencia() == null) {
   throw invalido("Idempotencia requerida");
  }

  idempotencyLocks.acquire(
          "pago", req.idempotencia()
  );

  var venta = ventas.bloquear(ventaId)
          .orElseThrow(() ->
                  new ResponseStatusException(
                          HttpStatus.NOT_FOUND
                  ));

  BigDecimal monto = dinero(req.monto());

  validarMedio(
          req.medio(),
          "PRESENCIAL".equals(venta.tipo)
  );

  var anterior = pagos.findByIdempotencia(
          req.idempotencia()
  );

  if (anterior.isPresent()) {
   var pago = anterior.get();

   if (!pago.ventaId.equals(ventaId)
           || !pago.responsableId.equals(adminId)
           || pago.monto.compareTo(monto) != 0
           || !pago.medio.equals(req.medio())
           || !Objects.equals(
           pago.referencia, req.referencia()
   )) {
    throw conflicto(
            "Clave de pago usada con otra solicitud"
    );
   }

   return pago;
  }

  if (!"ONLINE".equals(venta.tipo)
          || !"RECIBIDO".equals(venta.estado)
          || !"PENDIENTE".equals(venta.pagoEstado)
          || venta.vence == null
          || !venta.vence.isAfter(Instant.now())) {
   throw conflicto("Pedido no apto para cobrar");
  }

  if (venta.total.compareTo(monto) != 0) {
   throw conflicto(
           "Solo se acepta pago completo exacto"
   );
  }

  var filas = detalles.findByVentaId(venta.id);
  filas.sort(Comparator.comparing(d -> d.varianteId));

  for (var fila : filas) {
   var variante = variantes.bloquear(fila.varianteId)
           .orElseThrow();

   if (variante.stockReservado < fila.cantidad
           || variante.stockFisico < fila.cantidad) {
    throw conflicto(
            "Stock reservado inconsistente"
    );
   }

   variante.stockReservado -= fila.cantidad;
   variante.stockFisico -= fila.cantidad;
  }

  Pago pago = pagos.save(new Pago(
          venta.id,
          req.idempotencia(),
          monto,
          req.medio(),
          req.referencia(),
          adminId
  ));

  venta.pagoEstado = "CONFIRMADO";

  return pago;
 }

 // Devoluciones monetarias

 @Transactional
 public Devolucion devolver(
         Long pagoId,
         DevolucionRequest req,
         Long adminId
 ) {
  if (req == null || req.idempotencia() == null
          || req.motivo() == null
          || req.motivo().isBlank()) {
   throw invalido(
           "Motivo e idempotencia requeridos"
   );
  }

  BigDecimal monto = dinero(req.monto());

  idempotencyLocks.acquire(
          "devolucion", req.idempotencia()
  );

  var pagoInicial = pagos.findById(pagoId)
          .orElseThrow(() ->
                  new ResponseStatusException(
                          HttpStatus.NOT_FOUND
                  ));

  var venta = ventas.bloquear(pagoInicial.ventaId)
          .orElseThrow();

  var pago = pagos.findById(pagoId).orElseThrow();

  var anterior = devoluciones.findByIdempotencia(
          req.idempotencia()
  );

  if (anterior.isPresent()) {
   var devolucion = anterior.get();

   if (!devolucion.pagoId.equals(pagoId)
           || !devolucion.responsableId.equals(adminId)
           || devolucion.monto.compareTo(monto) != 0
           || !devolucion.motivo.equals(
           req.motivo().trim()
   )) {
    throw conflicto(
            "Clave de devolución usada con otra solicitud"
    );
   }

   return devolucion;
  }

  if (!"CONFIRMADO".equals(pago.estado)) {
   throw conflicto("Pago no reembolsable");
  }

  BigDecimal devuelto = devoluciones.findByPagoId(pagoId)
          .stream()
          .map(d -> d.monto)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

  if (devuelto.add(monto).compareTo(pago.monto) > 0) {
   throw conflicto(
           "Devolución supera el importe disponible"
   );
  }

  var resultado = devoluciones.save(new Devolucion(
          pagoId,
          req.idempotencia(),
          monto,
          req.motivo().trim(),
          adminId
  ));

  BigDecimal totalDevuelto = BigDecimal.ZERO;

  for (var cobro : pagos.findByVentaIdOrderByIdAsc(venta.id)) {
   for (var devolucion :
           devoluciones.findByPagoId(cobro.id)) {
    totalDevuelto = totalDevuelto.add(
            devolucion.monto
    );
   }
  }

  if (totalDevuelto.compareTo(venta.total) == 0) {
   venta.pagoEstado = "DEVUELTO";
  }

  // El reembolso no repone automáticamente el inventario.
  return resultado;
 }

 @Transactional(readOnly = true)
 public List<Pago> pagosDe(Long ventaId) {
  if (!ventas.existsById(ventaId)) {
   throw new ResponseStatusException(
           HttpStatus.NOT_FOUND
   );
  }

  return pagos.findByVentaIdOrderByIdAsc(ventaId);
 }
}