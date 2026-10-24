package com.tiendafrancis.auth.controller;

import com.tiendafrancis.auth.bean.dto.AuthDtos.*;
import com.tiendafrancis.auth.model.AuthModel;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/usuarios")
public class UsuariosController {

    private final AuthModel auth;

    private Long usuarioActual(
            Authentication authentication
    ) {

        return Long.valueOf(
                authentication.getName()
        );
    }

    // Consultar perfil

    @GetMapping("/me")
    public UsuarioDto perfil(
            Authentication authentication
    ) {

        return auth.perfil(
                usuarioActual(authentication)
        );
    }

    // Actualizar nombre propio

    @PutMapping("/me")
    public UsuarioDto cambiarNombre(

            Authentication authentication,

            @Valid
            @RequestBody
            NombreRequest request

    ) {

        return auth.actualizarNombre(
                usuarioActual(authentication),
                request.nombre()
        );
    }

    // Cambiar contraseña

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPassword(

            Authentication authentication,

            @Valid
            @RequestBody
            PasswordRequest request

    ) {

        auth.cambiarPassword(

                usuarioActual(authentication),

                request.actual(),

                request.nueva()
        );
    }

    // Listar usuarios: solo ADMIN

    @GetMapping
    public Page<UsuarioDto> listar(

            @RequestParam(defaultValue = "0")
            int pagina,

            @RequestParam(defaultValue = "20")
            int tamano

    ) {

        return auth.listar(
                pagina,
                tamano
        );
    }

    // Activar o desactivar cuentas: solo ADMIN

    @PatchMapping("/{id}/estado")
    public UsuarioDto cambiarEstado(

            Authentication authentication,

            @PathVariable
            Long id,

            @Valid
            @RequestBody
            EstadoRequest request

    ) {

        return auth.cambiarEstado(
                usuarioActual(authentication),
                id,
                request.activo()
        );
    }
}
