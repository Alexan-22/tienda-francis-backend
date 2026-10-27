
package com.tiendafrancis.comercio.controller;

import com.tiendafrancis.comercio.bean.entity.ProductoTalla;
import com.tiendafrancis.comercio.model.ComercioModel;
import com.tiendafrancis.comercio.persistence.VarianteRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ComercioController {

 private final ComercioModel model;
 private final VarianteRepository variantes;

 // =====================================================
 // DTO: TALLA Y STOCK DISPONIBLE
 // =====================================================

 public record TallaDto(
         Long idProductoTalla,
         String talla,
         int stockDisponible
 ) {
 }

 // =====================================================
 // DTO: PRODUCTO
 // =====================================================

 public record ProductoDto(
         Long id,
         String nombre,
         BigDecimal precio,
         BigDecimal precioOferta,
         List<TallaDto> tallas
 ) {
 }

 // =====================================================
 // SOLICITUDES
 // =====================================================

 public record CrearProducto(
         String nombre,
         BigDecimal precio
 ) {
 }

 public record CrearVariante(
         Long productoId,
         String talla,
         int stock
 ) {
 }

 public record Pedido(
         UUID idempotencia,
         List<ComercioModel.Item> items,
         Long direccionId
 ) {
 }

 public record VentaDto(
         Long id,
         BigDecimal total,
         String estado,
         String pagoEstado
 ) {
 }

 // =====================================================
 // CONVERTIR VARIANTE A DTO
 // =====================================================

 private TallaDto convertirTalla(
         ProductoTalla variante
 ) {

  // Se usa long para evitar desbordamiento
  // durante la resta antes de convertir a int.
  long disponibles = Math.max(
          0L,
          (long) variante.stockFisico
                  - variante.stockReservado
  );

  return new TallaDto(
          variante.id,
          variante.talla,
          (int) disponibles
  );
 }

 // =====================================================
 // CONSULTAR CATÁLOGO CON TALLAS
 // =====================================================

 @GetMapping("/api/productos")
 public List<ProductoDto> productos() {

  var catalogo = model.catalogo();

  if (catalogo.isEmpty()) {
   return List.of();
  }

  // IDs de productos activos del catálogo.
  List<Long> ids = catalogo.stream()
          .map(producto -> producto.id)
          .toList();

  // Consultar todas las variantes en una sola operación.
  List<ProductoTalla> registros =
          variantes.findByProductoIdIn(ids);

  Map<Long, List<TallaDto>> tallasPorProducto =
          new HashMap<>();

  for (ProductoTalla variante : registros) {

   tallasPorProducto
           .computeIfAbsent(
                   variante.productoId,
                   id -> new ArrayList<>()
           )
           .add(convertirTalla(variante));
  }

  // Ordenar las variantes por su identificador.
  for (List<TallaDto> lista : tallasPorProducto.values()) {

   lista.sort(
           Comparator.comparing(
                   TallaDto::idProductoTalla
           )
   );
  }

  // Construir la respuesta incluyendo las tallas.
  return catalogo.stream()
          .map(producto -> new ProductoDto(
                  producto.id,
                  producto.nombre,
                  producto.precio,
                  producto.precioOferta,
                  List.copyOf(
                          tallasPorProducto.getOrDefault(
                                  producto.id,
                                  List.of()
                          )
                  )
          ))
          .toList();
 }

 // =====================================================
 // CREAR PRODUCTO
 // =====================================================

 @PostMapping("/api/productos")
 @ResponseStatus(HttpStatus.CREATED)
 public ProductoDto crear(
         @RequestBody CrearProducto req
 ) {

  var producto = model.crearProducto(
          req.nombre(),
          req.precio()
  );

  return new ProductoDto(
          producto.id,
          producto.nombre,
          producto.precio,
          producto.precioOferta,
          List.of()
  );
 }

 // =====================================================
 // REGISTRAR TALLA
 // =====================================================

 @PostMapping("/api/producto-tallas")
 @ResponseStatus(HttpStatus.CREATED)
 public Map<String, Object> crearVariante(
         @RequestBody CrearVariante req
 ) {

  var variante = model.crearVariante(
          req.productoId(),
          req.talla(),
          req.stock()
  );

  return Map.of(
          "id", variante.id,
          "productoId", variante.productoId,
          "talla", variante.talla,
          "stock", variante.stockFisico
  );
 }

 // =====================================================
 // CREAR COMPRA ONLINE
 // =====================================================

 @PostMapping("/api/ventas")
 @ResponseStatus(HttpStatus.CREATED)
 public VentaDto comprar(
         @RequestBody Pedido req
 ) {

  var autenticacion = SecurityContextHolder
          .getContext()
          .getAuthentication();

  var venta = model.pedir(
          new ComercioModel.CrearVenta(
                  req.idempotencia(),
                  Long.parseLong(
                          autenticacion.getName()
                  ),
                  req.items(),
                  req.direccionId()
          )
  );

  return new VentaDto(
          venta.id,
          venta.total,
          venta.estado,
          venta.pagoEstado
  );
 }

 // =====================================================
 // ENDPOINT OBSOLETO
 // =====================================================

 @PostMapping("/api/ventas/{id}/confirmar-pago")
 public void confirmarObsoleto() {

  throw new ResponseStatusException(
          HttpStatus.GONE,
          "Usar POST /api/ventas/{id}/pagos"
  );
 }
}
