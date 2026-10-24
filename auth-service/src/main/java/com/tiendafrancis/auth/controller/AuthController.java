package com.tiendafrancis.auth.controller;

import com.tiendafrancis.auth.bean.dto.AuthDtos.*;
import com.tiendafrancis.auth.model.AuthModel;
import com.tiendafrancis.auth.security.JwtService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

 private final AuthModel auth;
 private final JwtService jwt;

 @PostMapping("/registro")
 @ResponseStatus(HttpStatus.CREATED)
 public Map<String, Long> registro(

         @Valid
         @RequestBody
         RegistroRequest datos

 ) {

  Long id = auth.registrar(
          datos.email(),
          datos.nombre(),
          datos.password()
  );

  return Map.of(
          "id",
          id
  );
 }

 @PostMapping("/login")
 public LoginResponse login(

         @Valid
         @RequestBody
         LoginRequest datos

 ) {

  String token = auth.login(
          datos.email(),
          datos.password()
  );

  return new LoginResponse(
          token,
          "Bearer",
          jwt.duracionSegundos()
  );
 }
}