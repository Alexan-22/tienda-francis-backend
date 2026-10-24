package com.tiendafrancis.comercio.controller;

import com.tiendafrancis.comercio.bean.entity.*;
import com.tiendafrancis.comercio.model.ComercioModel;
import com.tiendafrancis.comercio.persistence.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class PagosPosController {

 private final ComercioModel model;
 private final VentaRepository ventas;
 private final DetalleRepository detalles;
 private final VarianteRepository variantes;
 private final PagoRepository pagos;
 private final DevolucionRepository devoluciones;

 public record VentaPos(
         @NotNull UUID idempotencia,
         @NotBlank String cliente,
         @NotEmpty List<ComercioModel.Item> items,
         @NotBlank String medio
 ) {}

 public record Cobro(
         @NotNull UUID idempotencia,
         @NotNull @DecimalMin("0.01") BigDecimal monto,
         @NotBlank String medio,
         String referencia
 ) {}

 public record Reembolso(
         @NotNull UUID idempotencia,
         @NotNull @DecimalMin("0.01") BigDecimal monto,
         @NotBlank String motivo
 ) {}

 public record Anular(
         @NotBlank String motivo
 ) {}

 public record VentaRespuesta(
         Long id,
         String tipo,
         String estado,
         String pagoEstado,
         BigDecimal total
 ) {}

 public record PagoRespuesta(
         Long id,
         Long ventaId,
         UUID idempotencia,
         BigDecimal monto,
         String medio,
         String estado
 ) {}

 public record DevolucionRespuesta(
         Long id,
         Long pagoId,
         UUID idempotencia,
         BigDecimal monto,
         String motivo
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

 private VentaRespuesta dto(Venta v) {
  return new VentaRespuesta(
          v.id, v.tipo, v.estado, v.pagoEstado, v.total
  );
 }

 private PagoRespuesta dto(Pago p) {
  return new PagoRespuesta(
          p.id, p.ventaId, p.idempotencia,
          p.monto, p.medio, p.estado
  );
 }

 private DevolucionRespuesta dto(Devolucion d) {
  return new DevolucionRespuesta(
          d.id, d.pagoId, d.idempotencia,
          d.monto, d.motivo
  );
 }

 private ResponseStatusException conflicto(String mensaje) {
  return new ResponseStatusException(
          HttpStatus.CONFLICT, mensaje
  );
 }

 @PostMapping("/api/ventas/pos")
 @ResponseStatus(HttpStatus.CREATED)
 public VentaRespuesta pos(
         @Valid @RequestBody VentaPos req
 ) {
  var venta = model.venderPos(
          new ComercioModel.PosRequest(
                  req.idempotencia(),
                  req.cliente(),
                  req.items(),
                  req.medio(),
                  null
          ),
          admin()
  );

  return dto(venta);
 }

 @PostMapping("/api/ventas/{id}/pagos")
 @ResponseStatus(HttpStatus.CREATED)
 public PagoRespuesta cobrar(
         @PathVariable Long id,
         @Valid @RequestBody Cobro req
 ) {
  var pago = model.registrarPago(
          id,
          new ComercioModel.PagoRequest(
                  req.idempotencia(),
                  req.monto(),
                  req.medio(),
                  req.referencia()
          ),
          admin()
  );

  return dto(pago);
 }

 @PostMapping("/api/pagos/{pagoId}/devoluciones")
 @ResponseStatus(HttpStatus.CREATED)
 public DevolucionRespuesta devolver(
         @PathVariable Long pagoId,
         @Valid @RequestBody Reembolso req
 ) {
  return dto(model.devolver(
          pagoId,
          new ComercioModel.DevolucionRequest(
                  req.idempotencia(),
                  req.monto(),
                  req.motivo()
          ),
          admin()
  ));
 }

 @PostMapping("/api/pagos/{pagoId}/anular")
 @Transactional
 public PagoRespuesta anular(
         @PathVariable Long pagoId,
         @Valid @RequestBody Anular req
 ) {
  admin();

  var anterior = pagos.findById(pagoId)
          .orElseThrow(() ->
                  new ResponseStatusException(
                          HttpStatus.NOT_FOUND
                  ));

  var venta = ventas.bloquear(anterior.ventaId)
          .orElseThrow(() ->
                  new ResponseStatusException(
                          HttpStatus.NOT_FOUND
                  ));

  var pago = pagos.findById(pagoId).orElseThrow();

  if ("ANULADO".equals(pago.estado)) {
   return dto(pago);
  }

  if (!devoluciones.findByPagoId(pagoId).isEmpty()) {
   throw conflicto("Pago con devoluciones");
  }

  if ("CONFIRMADO".equals(venta.pagoEstado)) {
   throw conflicto(
           "Primero gestionar devolución comercial"
   );
  }

  pago.estado = "ANULADO";
  pago.anuladoEn = Instant.now();
  pago.motivoAnulacion = req.motivo();

  return dto(pago);
 }

 @GetMapping("/api/ventas/{id}/pagos")
 @Transactional(readOnly = true)
 public List<PagoRespuesta> listarPagos(
         @PathVariable Long id
 ) {
  admin();

  if (!ventas.existsById(id)) {
   throw new ResponseStatusException(
           HttpStatus.NOT_FOUND
   );
  }

  return pagos.findByVentaIdOrderByIdAsc(id).stream()
          .map(this::dto)
          .toList();
 }

 @GetMapping("/api/pagos/{pagoId}/devoluciones")
 @Transactional(readOnly = true)
 public List<DevolucionRespuesta> listarDevoluciones(
         @PathVariable Long pagoId
 ) {
  admin();

  if (!pagos.existsById(pagoId)) {
   throw new ResponseStatusException(
           HttpStatus.NOT_FOUND
   );
  }

  return devoluciones.findByPagoId(pagoId).stream()
          .map(this::dto)
          .toList();
 }
}