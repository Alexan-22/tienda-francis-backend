package com.tiendafrancis.comercio.controller;

import com.tiendafrancis.comercio.bean.entity.DireccionEnvio;
import com.tiendafrancis.comercio.model.DireccionesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/direcciones")
public class DireccionesController {

 private final DireccionesService service;

 public record DireccionDto(
         Long id,
         String destinatario,
         String linea,
         String distrito,
         String provincia,
         String departamento
 ) {}

 private Long usuario() {
  return Long.valueOf(
          SecurityContextHolder.getContext()
                  .getAuthentication()
                  .getName()
  );
 }

 private DireccionDto dto(DireccionEnvio d) {
  return new DireccionDto(
          d.id, d.destinatario, d.linea,
          d.distrito, d.provincia, d.departamento
  );
 }

 @PostMapping
 @ResponseStatus(HttpStatus.CREATED)
 public DireccionDto crear(
         @RequestBody DireccionesService.Datos input
 ) {
  return dto(service.crear(usuario(), input));
 }

 @GetMapping
 public List<DireccionDto> listar() {
  return service.listar(usuario()).stream()
          .map(this::dto)
          .toList();
 }

 @DeleteMapping("/{id}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void borrar(@PathVariable Long id) {
  service.desactivar(usuario(), id);
 }
}
