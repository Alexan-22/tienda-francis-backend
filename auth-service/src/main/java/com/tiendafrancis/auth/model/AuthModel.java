package com.tiendafrancis.auth.model;

import com.tiendafrancis.auth.bean.dto.AuthDtos.UsuarioDto;
import com.tiendafrancis.auth.bean.entity.Usuario;
import com.tiendafrancis.auth.persistence.UsuarioRepository;
import com.tiendafrancis.auth.security.JwtService;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthModel {

 private final UsuarioRepository usuarios;
 private final PasswordEncoder encoder;
 private final JwtService jwt;

 private String normalizarEmail(String email) {

  if (email == null || email.isBlank()) {

   throw new ResponseStatusException(
           HttpStatus.BAD_REQUEST,
           "Correo requerido"
   );
  }

  String normalizado = email.trim()
          .toLowerCase(Locale.ROOT);

  if (normalizado.length() > 255

          || !normalizado.matches(
          "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
  )) {

   throw new ResponseStatusException(
           HttpStatus.BAD_REQUEST,
           "Correo inválido"
   );
  }

  return normalizado;
 }

 private String validarNombre(String nombre) {

  if (nombre == null
          || nombre.isBlank()
          || nombre.length() > 255) {

   throw new ResponseStatusException(
           HttpStatus.BAD_REQUEST,
           "Nombre inválido"
   );
  }

  return nombre.trim();
 }

 private void validarPassword(String password) {

  if (password == null || password.isBlank()) {

   throw new ResponseStatusException(
           HttpStatus.BAD_REQUEST,
           "Contraseña requerida"
   );
  }

  int bytes = password.getBytes(
          StandardCharsets.UTF_8
  ).length;

  if (bytes < 8 || bytes > 72) {

   throw new ResponseStatusException(
           HttpStatus.BAD_REQUEST,
           "La contraseña debe ocupar entre 8 y 72 bytes UTF-8"
   );
  }
 }

 // Registro público: siempre CLIENTE

 @Transactional
 public Long registrar(
         String email,
         String nombre,
         String password
 ) {

  String correo = normalizarEmail(email);

  String nombreSeguro = validarNombre(nombre);

  validarPassword(password);

  if (usuarios.existsByEmail(correo)) {

   throw new ResponseStatusException(
           HttpStatus.CONFLICT,
           "Correo ya registrado"
   );
  }

  try {

   Usuario usuario = new Usuario(
           correo,
           encoder.encode(password),
           nombreSeguro
   );

   return usuarios.saveAndFlush(usuario).getId();

  } catch (DataIntegrityViolationException ex) {

   throw new ResponseStatusException(
           HttpStatus.CONFLICT,
           "Correo ya registrado"
   );
  }
 }

 // Inicio de sesión

 @Transactional(readOnly = true)
 public String login(
         String email,
         String password
 ) {

  String correo = normalizarEmail(email);

  Usuario usuario = usuarios.findByEmail(correo)

          .orElseThrow(() ->
                  new ResponseStatusException(
                          HttpStatus.UNAUTHORIZED,
                          "Credenciales inválidas"
                  )
          );

  if (password == null
          || password.isBlank()
          || password.getBytes(
          StandardCharsets.UTF_8
  ).length > 72
          || !usuario.isActivo()
          || !encoder.matches(
          password,
          usuario.getPasswordHash()
  )) {

   throw new ResponseStatusException(
           HttpStatus.UNAUTHORIZED,
           "Credenciales inválidas"
   );
  }

  return jwt.emitir(usuario);
 }

 // Verificación del JWT

 @Transactional(readOnly = true)
 public Usuario verificar(String token) {

  return jwt.verificar(token);
 }

 // Perfil del usuario autenticado

 @Transactional(readOnly = true)
 public UsuarioDto perfil(Long id) {

  return UsuarioDto.desde(
          obtener(id)
  );
 }

 // Actualizar nombre

 @Transactional
 public UsuarioDto actualizarNombre(
         Long id,
         String nuevoNombre
 ) {

  Usuario usuario = obtener(id);

  usuario.actualizarNombre(
          validarNombre(nuevoNombre)
  );

  return UsuarioDto.desde(usuario);
 }

 // Cambio de contraseña y revocación de tokens anteriores

 @Transactional
 public void cambiarPassword(
         Long id,
         String actual,
         String nueva
 ) {

  Usuario usuario = obtener(id);

  if (actual == null
          || !encoder.matches(
          actual,
          usuario.getPasswordHash()
  )) {

   throw new ResponseStatusException(
           HttpStatus.UNAUTHORIZED,
           "Contraseña actual incorrecta"
   );
  }

  validarPassword(nueva);

  if (encoder.matches(
          nueva,
          usuario.getPasswordHash()
  )) {

   throw new ResponseStatusException(
           HttpStatus.BAD_REQUEST,
           "Elige una contraseña diferente"
   );
  }

  usuario.cambiarPassword(
          encoder.encode(nueva)
  );
 }

 // Listado de usuarios para ADMIN

 @Transactional(readOnly = true)
 public Page<UsuarioDto> listar(
         int pagina,
         int tamano
 ) {

  if (pagina < 0
          || tamano < 1
          || tamano > 50) {

   throw new ResponseStatusException(
           HttpStatus.BAD_REQUEST,
           "Paginación inválida"
   );
  }

  return usuarios.findAll(
          PageRequest.of(
                  pagina,
                  tamano,
                  Sort.by("id").descending()
          )
  ).map(UsuarioDto::desde);
 }

 // Activar o desactivar cuentas

 @Transactional
 public UsuarioDto cambiarEstado(
         Long adminId,
         Long id,
         boolean activo
 ) {

  Usuario usuario = obtener(id);

  if (!activo && adminId.equals(id)) {

   throw new ResponseStatusException(
           HttpStatus.CONFLICT,
           "No puedes desactivar tu propia cuenta"
   );
  }

  if (!activo && "ADMIN".equals(usuario.getRol())) {

   throw new ResponseStatusException(
           HttpStatus.CONFLICT,
           "Este endpoint no desactiva administradores"
   );
  }

  usuario.cambiarActivo(activo);

  return UsuarioDto.desde(usuario);
 }

 // Administrador inicial explícito

 @Transactional
 public Long crearAdminInicial(
         String email,
         String nombre,
         String password
 ) {

  String correo = normalizarEmail(email);

  String nombreSeguro = validarNombre(nombre);

  validarPassword(password);

  var existente = usuarios.findByEmail(correo);

  if (existente.isPresent()) {

   Usuario usuario = existente.get();

   if (!"ADMIN".equals(usuario.getRol())
           || !usuario.isActivo()) {

    throw new IllegalStateException(
            "Existe una cuenta con ADMIN_EMAIL " +
                    "y no es admin activo; no se modificará"
    );
   }

   return usuario.getId();
  }

  Usuario usuario = new Usuario(
          correo,
          encoder.encode(password),
          nombreSeguro
  );

  usuario.convertirEnAdminInicial();

  return usuarios.saveAndFlush(usuario).getId();
 }

 // Búsqueda interna reutilizable

 private Usuario obtener(Long id) {

  if (id == null) {

   throw new ResponseStatusException(
           HttpStatus.UNAUTHORIZED
   );
  }

  return usuarios.findById(id)

          .orElseThrow(() ->
                  new ResponseStatusException(
                          HttpStatus.NOT_FOUND,
                          "Usuario no encontrado"
                  )
          );
 }
}
