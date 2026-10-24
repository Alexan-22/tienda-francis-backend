package com.tiendafrancis.comercio.controller;

import com.tiendafrancis.comercio.model.ComercioModel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class ComercioController {

 private final ComercioModel model;

 public record ProductoDto(
         Long id,
         String nombre,
         BigDecimal precio,
         BigDecimal precioOferta
 ) {}

 public record CrearProducto(
         String nombre,
         BigDecimal precio
 ) {}

 public record CrearVariante(
         Long productoId,
         String talla,
         int stock
 ) {}

 public record Pedido(
         UUID idempotencia,
         List<ComercioModel.Item> items,
         Long direccionId
 ) {}

 public record VentaDto(
         Long id,
         BigDecimal total,
         String estado,
         String pagoEstado
 ) {}

 @GetMapping("/api/productos")
 public List<ProductoDto> productos() {
  return model.catalogo().stream()
          .map(p -> new ProductoDto(
                  p.id, p.nombre, p.precio, p.precioOferta
          ))
          .toList();
 }

 @PostMapping("/api/productos")
 @ResponseStatus(HttpStatus.CREATED)
 public ProductoDto crear(@RequestBody CrearProducto req) {
  var p = model.crearProducto(req.nombre(), req.precio());

  return new ProductoDto(
          p.id, p.nombre, p.precio, p.precioOferta
  );
 }

 @PostMapping("/api/producto-tallas")
 @ResponseStatus(HttpStatus.CREATED)
 public Map<String, Object> crearVariante(
         @RequestBody CrearVariante req
 ) {
  var v = model.crearVariante(
          req.productoId(), req.talla(), req.stock()
  );

  return Map.of(
          "id", v.id,
          "productoId", v.productoId,
          "talla", v.talla,
          "stock", v.stockFisico
  );
 }

 @PostMapping("/api/ventas")
 @ResponseStatus(HttpStatus.CREATED)
 public VentaDto comprar(@RequestBody Pedido req) {
  var auth = SecurityContextHolder.getContext()
          .getAuthentication();

  var venta = model.pedir(new ComercioModel.CrearVenta(
          req.idempotencia(),
          Long.parseLong(auth.getName()),
          req.items(),
          req.direccionId()
  ));

  return new VentaDto(
          venta.id,
          venta.total,
          venta.estado,
          venta.pagoEstado
  );
 }

 @PostMapping("/api/ventas/{id}/confirmar-pago")
 public void confirmarObsoleto() {
  throw new ResponseStatusException(
          HttpStatus.GONE,
          "Usar POST /api/ventas/{id}/pagos"
  );
 }
}