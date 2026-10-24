package com.tiendafrancis.comercio.bean.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ventas")
public class Venta {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 public Long id;

 @Column(nullable = false, unique = true)
 public UUID idempotencia;

 public Long usuarioId;
 public Long responsableId;
 public String clientePresencialNombre;

 @Column(length = 64)
 public String solicitudHash;

 @Column(nullable = false)
 public String tipo;

 @Column(nullable = false)
 public String estado = "RECIBIDO";

 @Column(nullable = false)
 public String pagoEstado = "PENDIENTE";

 @Column(nullable = false, precision = 12, scale = 2)
 public BigDecimal total;

 @Column(nullable = false)
 public Instant creada = Instant.now();

 public Instant vence;
 public Long direccionId;

 @Column(columnDefinition = "text")
 public String direccionSnapshot;

 @Column(precision = 12, scale = 2)
 public BigDecimal subtotal;

 @Column(precision = 12, scale = 2)
 public BigDecimal deliveryCosto;

 public Venta(
         UUID key,
         Long user,
         String tipo,
         BigDecimal total,
         Instant vence
 ) {
  this.idempotencia = key;
  this.usuarioId = user;
  this.tipo = tipo;
  this.total = total;
  this.vence = vence;
 }
}