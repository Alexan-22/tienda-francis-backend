package com.tiendafrancis.comercio.bean.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "recepciones_devolucion")
public class RecepcionDevolucion {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 public Long id;

 @Column(nullable = false)
 public Long ventaId;

 @Column(nullable = false)
 public Long detalleId;

 @Column(nullable = false, unique = true)
 public UUID idempotencia;

 @Column(nullable = false)
 public int cantidad;

 @Column(nullable = false, length = 20)
 public String condicion;

 @Column(nullable = false, length = 500)
 public String motivo;

 @Column(nullable = false)
 public Long responsableId;

 @Column(nullable = false)
 public boolean repuesto;

 @Column(nullable = false)
 public Instant recibidoEn = Instant.now();

 public RecepcionDevolucion(
         Long ventaId,
         Long detalleId,
         UUID key,
         int cantidad,
         String condicion,
         String motivo,
         Long actor,
         boolean repuesto
 ) {
  this.ventaId = ventaId;
  this.detalleId = detalleId;
  this.idempotencia = key;
  this.cantidad = cantidad;
  this.condicion = condicion;
  this.motivo = motivo;
  this.responsableId = actor;
  this.repuesto = repuesto;
 }
}
