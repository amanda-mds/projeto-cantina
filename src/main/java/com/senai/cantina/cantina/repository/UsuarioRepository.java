package com.senai.cantina.cantina.repository;

import java.util.List;
import java.util.Optional;

import com.senai.cantina.cantina.model.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository
        extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            SELECT u
            FROM Usuario u
            WHERE LOWER(u.email) = LOWER(:login)
            OR LOWER(u.nome) = LOWER(:login)
            """)
    List<Usuario> buscarPorEmailOuNome(
            @Param("login") String login
    );
}