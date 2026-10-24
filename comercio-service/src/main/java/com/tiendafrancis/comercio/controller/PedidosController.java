package com.tiendafrancis.comercio.controller;

import com.tiendafrancis.comercio.bean.entity.*;
import com.tiendafrancis.comercio.model.PedidosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class PedidosController {

 private final PedidosService pedidos;

 public record EstadoInput(
         String estado,
         String motivo
 ) {}

 public record PedidoDto(
         Long id,
         String estado,
         String pagoEstado,
         BigDecimal total,
         Instant vence
 ) {}

 public record EventoDto(
         String origen,
         String destino,
         String motivo,
         Long responsableId,
         Instant ocurridoEn
 ) {}

 private Authentication auth() {
  return SecurityContextHolder.getContext()
          .getAuthentication();
 }

 private Long usuario() {
  return Long.valueOf(auth().getName());
 }

 private boolean admin() {
  return auth().getAuthorities().stream()
          .anyMatch(a ->
                  "ROLE_ADMIN".equals(a.getAuthority())
          );
 }

 private PedidoDto dto(Venta v) {
  return new PedidoDto(
          v.id, v.estado, v.pagoEstado, v.total, v.vence
  );
 }

 @GetMapping("/api/ventas/mis-pedidos")
 public List<PedidoDto> propios() {
  return pedidos.misPedidos(usuario()).stream()
          .map(this::dto)
          .toList();
 }

 @GetMapping("/api/ventas/{id}/seguimiento")
 public List<EventoDto> seguimiento(@PathVariable Long id) {
  return pedidos.seguimiento(
                  id, usuario(), admin()
          ).stream()
          .map(h -> new EventoDto(
                  h.origen, h.destino, h.motivo,
                  h.responsableId, h.ocurridoEn
          ))
          .toList();
 }

 @PostMapping("/api/ventas/{id}/estado")
 public PedidoDto cambiar(
         @PathVariable Long id,
         @RequestBody EstadoInput datos
 ) {
  if (!admin()) {
   throw new ResponseStatusException(
           HttpStatus.FORBIDDEN
   );
  }

  return dto(pedidos.cambiar(
          id,
          datos == null ? null : datos.estado(),
          usuario(),
          datos == null ? null : datos.motivo()
  ));
 }
}