package com.tiendafrancis.comercio.controller;

import com.tiendafrancis.comercio.model.ReportesService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reportes")
@PreAuthorize("hasRole('ADMIN')")
public class ReportesController {

 private final ReportesService reportes;

 private ReportesService.Periodo periodo(
         LocalDate desde,
         LocalDate hasta
 ) {
  return reportes.periodo(desde, hasta);
 }

 @GetMapping("/ventas")
 public Map<String, Object> ventas(
         @RequestParam
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
         LocalDate desde,

         @RequestParam
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
         LocalDate hasta
 ) {
  return reportes.ventas(periodo(desde, hasta));
 }

 @GetMapping("/caja")
 public Map<String, Object> caja(
         @RequestParam
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
         LocalDate desde,

         @RequestParam
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
         LocalDate hasta
 ) {
  return reportes.caja(periodo(desde, hasta));
 }

 @GetMapping("/inventario")
 public Map<String, Object> inventario(
         @RequestParam(defaultValue = "5") int umbral
 ) {
  return reportes.inventario(umbral);
 }

 @GetMapping("/pagos")
 public Map<String, Object> pagos(
         @RequestParam
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
         LocalDate desde,

         @RequestParam
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
         LocalDate hasta
 ) {
  return reportes.pagos(periodo(desde, hasta));
 }

 @GetMapping("/devoluciones")
 public Map<String, Object> devoluciones(
         @RequestParam
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
         LocalDate desde,

         @RequestParam
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
         LocalDate hasta
 ) {
  return reportes.devoluciones(periodo(desde, hasta));
 }
}