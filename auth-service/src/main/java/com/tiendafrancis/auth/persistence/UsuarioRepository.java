package com.tiendafrancis.auth.persistence;

import com.tiendafrancis.auth.bean.entity.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository
        extends JpaRepository<Usuario, Long> {

 Optional<Usuario> findByEmail(String email);

 boolean existsByEmail(String email);
}