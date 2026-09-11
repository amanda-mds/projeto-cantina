package com.senai.cantina.cantina.service;

import java.util.List;

import com.senai.cantina.cantina.model.Usuario;
import com.senai.cantina.cantina.repository.UsuarioRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioUserDetailsService
        implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioUserDetailsService(
            UsuarioRepository usuarioRepository
    ) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login)
            throws UsernameNotFoundException {

        if (login == null || login.isBlank()) {
            throw new UsernameNotFoundException(
                    "Informe o nome ou e-mail."
            );
        }

        String loginLimpo = login.trim();

        List<Usuario> encontrados =
                usuarioRepository.buscarPorEmailOuNome(
                        loginLimpo
                );

        if (encontrados.isEmpty()) {
            throw new UsernameNotFoundException(
                    "Usuário não encontrado: "
                            + loginLimpo
            );
        }

        if (encontrados.size() > 1) {
            throw new UsernameNotFoundException(
                    "Existe mais de um usuário com esse nome. Entre com o e-mail."
            );
        }

        Usuario usuario = encontrados.get(0);

        if (!usuario.isAtivo()) {
            throw new UsernameNotFoundException(
                    "Este usuário está inativo."
            );
        }

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getSenha())
                .roles(
                        usuario.getTipoUsuario().name()
                )
                .build();
    }
}