package com.tiendafrancis.comercio.bean.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "movimientos_inventario")
public class MovimientoInventario {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 public Long id;

 @Column(nullable = false)
 public Long varianteId;

 @Column(nullable = false)
 public Long recepcionId;

 @Column(nullable = false)
 public int cantidad;

 @Column(nullable = false, length = 30)
 public String tipo;

 @Column(nullable = false, length = 500)
 public String motivo;

 @Column(nullable = false)
 public Long responsableId;

 @Column(nullable = false)
 public Instant creadoEn = Instant.now();

 public MovimientoInventario(
         Long variante,
         Long recepcion,
         int cantidad,
         String motivo,
         Long actor
 ) {
  this.varianteId = variante;
  this.recepcionId = recepcion;
  this.cantidad = cantidad;
  this.tipo = "REPOSICION_DEVOLUCION";
  this.motivo = motivo;
  this.responsableId = actor;
 }
}