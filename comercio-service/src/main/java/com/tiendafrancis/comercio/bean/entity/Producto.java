package com.tiendafrancis.comercio.bean.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "productos")
public class Producto {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 public Long id;

 @Column(nullable = false)
 public String nombre;

 @Column(nullable = false, precision = 12, scale = 2)
 public BigDecimal precio;

 @Column(precision = 12, scale = 2)
 public BigDecimal precioOferta;

 @Column(nullable = false)
 public boolean activo = true;

 public Producto(String nombre, BigDecimal precio) {
  this.nombre = nombre;
  this.precio = precio;
 }
}