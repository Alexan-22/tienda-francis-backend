package com.tiendafrancis.comercio.bean.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "devoluciones")
public class Devolucion {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 public Long id;

 @Column(nullable = false)
 public Long pagoId;

 @Column(nullable = false, unique = true)
 public UUID idempotencia;

 @Column(nullable = false, precision = 12, scale = 2)
 public BigDecimal monto;

 @Column(nullable = false)
 public String motivo;

 @Column(nullable = false)
 public Long responsableId;

 @Column(nullable = false)
 public Instant creada = Instant.now();

 public Devolucion(
         Long pagoId,
         UUID key,
         BigDecimal monto,
         String motivo,
         Long actor
 ) {
  this.pagoId = pagoId;
  this.idempotencia = key;
  this.monto = monto;
  this.motivo = motivo;
  this.responsableId = actor;
 }
}