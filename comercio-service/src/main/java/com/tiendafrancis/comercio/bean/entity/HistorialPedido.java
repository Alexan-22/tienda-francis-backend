package com.tiendafrancis.comercio.bean.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "historial_pedidos")
public class HistorialPedido {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 public Long id;

 @Column(nullable = false)
 public Long ventaId;

 @Column(nullable = false, length = 30)
 public String origen;

 @Column(nullable = false, length = 30)
 public String destino;

 public Long responsableId;

 @Column(nullable = false, length = 500)
 public String motivo;

 @Column(nullable = false)
 public Instant ocurridoEn;

 public HistorialPedido(
         Long venta,
         String origen,
         String destino,
         Long responsable,
         String motivo,
         Instant momento
 ) {
  this.ventaId = venta;
  this.origen = origen;
  this.destino = destino;
  this.responsableId = responsable;
  this.motivo = motivo;
  this.ocurridoEn = momento;
 }
}
