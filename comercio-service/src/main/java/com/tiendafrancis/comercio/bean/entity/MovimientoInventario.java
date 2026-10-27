
package com.tiendafrancis.comercio.bean.entity;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "movimientos_inventario")
public class MovimientoInventario {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 public Long id;

 @Column(name = "variante_id", nullable = false)
 public Long varianteId;

 @Column(name = "recepcion_id")
 public Long recepcionId;

 @Column(name = "idempotencia")
 public UUID idempotencia;

 @Column(nullable = false)
 public int cantidad;

 @Column(nullable = false, length = 30)
 public String tipo;

 @Column(nullable = false, length = 500)
 public String motivo;

 @Column(name = "responsable_id", nullable = false)
 public Long responsableId;

 @Column(name = "creado_en", nullable = false)
 public Instant creadoEn = Instant.now();

 // =================================================
 // REPOSICIÓN POR DEVOLUCIÓN EXISTENTE
 // =================================================

 public MovimientoInventario(
         Long variante,
         Long recepcion,
         int cantidad,
         String motivo,
         Long actor
 ) {
  this.varianteId = variante;
  this.recepcionId = recepcion;
  this.idempotencia = null;
  this.cantidad = cantidad;
  this.tipo = "REPOSICION_DEVOLUCION";
  this.motivo = motivo;
  this.responsableId = actor;
 }

 // =================================================
 // NUEVA REPOSICIÓN MANUAL
 // =================================================

 public MovimientoInventario(
         Long variante,
         UUID idempotencia,
         int cantidad,
         String motivo,
         Long actor
 ) {
  this.varianteId = variante;
  this.recepcionId = null;
  this.idempotencia = idempotencia;
  this.cantidad = cantidad;
  this.tipo = "REPOSICION_MANUAL";
  this.motivo = motivo;
  this.responsableId = actor;
 }
}
