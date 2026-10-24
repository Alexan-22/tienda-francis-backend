package com.tiendafrancis.auth.bean.dto;

import com.tiendafrancis.auth.bean.entity.Usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegistroRequest(

            @NotBlank
            @Email
            String email,

            @NotBlank
            @Size(max = 255)
            String nombre,

            @NotBlank
            String password

    ) {}

    public record LoginRequest(

            @NotBlank
            String email,

            @NotBlank
            String password

    ) {}

    public record LoginResponse(

            String token,
            String tipo,
            long expiraEnSegundos

    ) {}

    public record UsuarioDto(

            Long id,
            String email,
            String nombre,
            String rol,
            boolean activo

    ) {

        public static UsuarioDto desde(Usuario usuario) {

            return new UsuarioDto(
                    usuario.getId(),
                    usuario.getEmail(),
                    usuario.getNombre(),
                    usuario.getRol(),
                    usuario.isActivo()
            );
        }
    }

    public record NombreRequest(

            @NotBlank
            @Size(max = 255)
            String nombre

    ) {}

    public record PasswordRequest(

            @NotBlank
            String actual,

            @NotBlank
            String nueva

    ) {}

    public record EstadoRequest(

            @NotNull
            Boolean activo

    ) {}

    public record IntrospeccionDto(

            Long id,
            String rol,
            boolean activo

    ) {}
}