package com.tiendafrancis.comercio.bean.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "producto_tallas",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"producto_id", "talla"}
        )
)
public class ProductoTalla {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 public Long id;

 @Column(name = "producto_id", nullable = false)
 public Long productoId;

 @Column(nullable = false)
 public String talla;

 @Column(nullable = false)
 public int stockFisico;

 @Column(nullable = false)
 public int stockReservado;

 @Version
 public long version;

 public ProductoTalla(Long productoId, String talla, int stock) {
  this.productoId = productoId;
  this.talla = talla;
  this.stockFisico = stock;
 }
}