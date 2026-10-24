package com.tiendafrancis.comercio.bean.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "pagos")
public class Pago {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 public Long id;

 @Column(nullable = false)
 public Long ventaId;

 @Column(nullable = false, unique = true)
 public UUID idempotencia;

 @Column(nullable = false, precision = 12, scale = 2)
 public BigDecimal monto;

 @Column(nullable = false)
 public String medio;

 public String referencia;

 @Column(nullable = false)
 public Long responsableId;

 @Column(nullable = false)
 public String estado = "CONFIRMADO";

 @Column(nullable = false)
 public Instant creado = Instant.now();

 public Instant anuladoEn;
 public String motivoAnulacion;

 public Pago(
         Long ventaId,
         UUID key,
         BigDecimal monto,
         String medio,
         String referencia,
         Long responsableId
 ) {
  this.ventaId = ventaId;
  this.idempotencia = key;
  this.monto = monto;
  this.medio = medio;
  this.referencia = referencia;
  this.responsableId = responsableId;
 }
}
