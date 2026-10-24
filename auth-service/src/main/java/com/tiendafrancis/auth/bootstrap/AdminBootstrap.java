package com.tiendafrancis.auth.bootstrap;

import com.tiendafrancis.auth.model.AuthModel;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "auth.bootstrap",
        name = "enabled",
        havingValue = "true"
)
public class AdminBootstrap implements ApplicationRunner {

    private final AuthModel auth;

    @Value("${auth.bootstrap.email:}")
    private String email;

    @Value("${auth.bootstrap.nombre:Administrador}")
    private String nombre;

    @Value("${auth.bootstrap.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {

        auth.crearAdminInicial(
                email,
                nombre,
                password
        );
    }
}