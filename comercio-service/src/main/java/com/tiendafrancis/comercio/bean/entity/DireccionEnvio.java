package com.tiendafrancis.comercio.bean.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "direcciones_envio")
public class DireccionEnvio {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 public Long id;

 @Column(nullable = false)
 public Long usuarioId;

 @Column(nullable = false, length = 120)
 public String destinatario;

 @Column(nullable = false, length = 255)
 public String linea;

 @Column(nullable = false, length = 100)
 public String distrito;

 @Column(nullable = false, length = 100)
 public String provincia;

 @Column(nullable = false, length = 100)
 public String departamento;

 @Column(nullable = false)
 public boolean activa = true;

 public DireccionEnvio(
         Long usuario,
         String destinatario,
         String linea,
         String distrito,
         String provincia,
         String departamento
 ) {
  this.usuarioId = usuario;
  this.destinatario = destinatario;
  this.linea = linea;
  this.distrito = distrito;
  this.provincia = provincia;
  this.departamento = departamento;
 }
}
