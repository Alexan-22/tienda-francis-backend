package com.tiendafrancis.comercio.model;

import com.tiendafrancis.comercio.bean.entity.DireccionEnvio;
import com.tiendafrancis.comercio.persistence.DireccionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DireccionesService {

 private final DireccionRepository repo;

 public record Datos(
         String destinatario,
         String linea,
         String distrito,
         String provincia,
         String departamento
 ) {}

 private String requerido(String valor) {
  if (valor == null || valor.isBlank()
          || valor.length() > 100) {
   throw new ResponseStatusException(
           HttpStatus.BAD_REQUEST,
           "Datos de dirección inválidos"
   );
  }

  return valor.trim();
 }

 @Transactional
 public DireccionEnvio crear(Long usuario, Datos datos) {
  if (datos == null) {
   throw new ResponseStatusException(
           HttpStatus.BAD_REQUEST
   );
  }

  return repo.save(new DireccionEnvio(
          usuario,
          requerido(datos.destinatario()),
          requerido(datos.linea()),
          requerido(datos.distrito()),
          requerido(datos.provincia()),
          requerido(datos.departamento())
  ));
 }

 @Transactional(readOnly = true)
 public List<DireccionEnvio> listar(Long usuario) {
  return repo.findByUsuarioIdAndActivaTrueOrderByIdAsc(
          usuario
  );
 }

 @Transactional
 public void desactivar(Long usuario, Long id) {
  var direccion = repo.findByIdAndUsuarioIdAndActivaTrue(
          id, usuario
  ).orElseThrow(() ->
          new ResponseStatusException(HttpStatus.NOT_FOUND)
  );

  direccion.activa = false;
 }
}
