package com.tiendafrancis.comercio.controller;

import com.tiendafrancis.comercio.bean.entity.RecepcionDevolucion;
import com.tiendafrancis.comercio.model.PostventaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PostventaController {

 private final PostventaService postventa;

 public record Cancelacion(String motivo) {}

 public record VentaDto(
         Long id,
         String estado,
         String pagoEstado
 ) {}

 public record RecepcionDto(
         Long id,
         Long ventaId,
         Long detalleId,
         int cantidad,
         String condicion,
         boolean repuesto
 ) {}

 private Long admin() {
  var auth = SecurityContextHolder.getContext()
          .getAuthentication();

  if (auth == null || auth.getAuthorities().stream()
          .noneMatch(a ->
                  "ROLE_ADMIN".equals(a.getAuthority())
          )) {
   throw new ResponseStatusException(
           HttpStatus.FORBIDDEN
   );
  }

  return Long.valueOf(auth.getName());
 }

 private RecepcionDto dto(RecepcionDevolucion r) {
  return new RecepcionDto(
          r.id,
          r.ventaId,
          r.detalleId,
          r.cantidad,
          r.condicion,
          r.repuesto
  );
 }

 @PostMapping("/api/ventas/{id}/cancelar")
 public VentaDto cancelar(
         @PathVariable Long id,
         @RequestBody Cancelacion datos
 ) {
  var venta = postventa.cancelar(
          id,
          admin(),
          datos == null ? null : datos.motivo()
  );

  return new VentaDto(
          venta.id, venta.estado, venta.pagoEstado
  );
 }

 @PostMapping("/api/ventas/{id}/recepciones")
 @ResponseStatus(HttpStatus.CREATED)
 public RecepcionDto recibir(
         @PathVariable Long id,
         @RequestBody PostventaService.RecibirRequest datos
 ) {
  return dto(postventa.recibir(id, datos, admin()));
 }

 @GetMapping("/api/ventas/{id}/recepciones")
 public List<RecepcionDto> listar(@PathVariable Long id) {
  admin();

  return postventa.recepciones(id).stream()
          .map(this::dto)
          .toList();
 }
}