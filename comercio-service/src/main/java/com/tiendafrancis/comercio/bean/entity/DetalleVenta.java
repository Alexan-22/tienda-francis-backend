package com.tiendafrancis.comercio.bean.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "detalle_ventas")
public class DetalleVenta {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 public Long id;

 @Column(nullable = false)
 public Long ventaId;

 @Column(nullable = false)
 public Long varianteId;

 @Column(nullable = false)
 public String productoNombre;

 @Column(nullable = false)
 public String talla;

 @Column(nullable = false)
 public int cantidad;

 @Column(nullable = false, precision = 12, scale = 2)
 public BigDecimal precioUnitario;

 public DetalleVenta(
         Long ventaId,
         Long varianteId,
         String nombre,
         String talla,
         int cantidad,
         BigDecimal precio
 ) {
  this.ventaId = ventaId;
  this.varianteId = varianteId;
  this.productoNombre = nombre;
  this.talla = talla;
  this.cantidad = cantidad;
  this.precioUnitario = precio;
 }
}