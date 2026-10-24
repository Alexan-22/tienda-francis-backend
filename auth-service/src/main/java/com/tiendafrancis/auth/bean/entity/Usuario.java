package com.tiendafrancis.auth.bean.entity;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "usuarios")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @Column(
         nullable = false,
         unique = true,
         length = 255
 )
 private String email;

 @Column(
         name = "password_hash",
         nullable = false,
         length = 255
 )
 private String passwordHash;

 @Column(
         nullable = false,
         length = 255
 )
 private String nombre;

 @Column(
         nullable = false,
         length = 30
 )
 private String rol = "CLIENTE";

 @Column(nullable = false)
 private boolean activo = true;

 @Column(
         name = "token_version",
         nullable = false
 )
 private long tokenVersion = 0;

 public Usuario(
         String email,
         String passwordHash,
         String nombre
 ) {
  this.email = email;
  this.passwordHash = passwordHash;
  this.nombre = nombre;
 }

 public void actualizarNombre(String nuevoNombre) {
  this.nombre = nuevoNombre;
 }

 public void cambiarPassword(String nuevoHash) {
  this.passwordHash = nuevoHash;
  this.tokenVersion++;
 }

 public void cambiarActivo(boolean estado) {

  if (this.activo != estado) {
   this.activo = estado;
   this.tokenVersion++;
  }
 }

 public void convertirEnAdminInicial() {
  this.rol = "ADMIN";
 }
}
