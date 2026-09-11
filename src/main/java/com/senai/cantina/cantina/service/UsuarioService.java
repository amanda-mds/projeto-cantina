package com.senai.cantina.cantina.service;

import com.senai.cantina.cantina.exception.RegraNegocioException;

import java.util.List;

import com.senai.cantina.cantina.exception.RecursoNaoEncontradoException;
import com.senai.cantina.cantina.model.Usuario;
import com.senai.cantina.cantina.model.Usuario.TipoUsuario;
import com.senai.cantina.cantina.repository.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario cadastrarCliente(Usuario usuario) {

        String email =
                usuario.getEmail().trim().toLowerCase();

        usuario.setEmail(email);

        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraNegocioException(
                    "Já existe um usuário cadastrado com esse e-mail."
            );
        }

        usuario.setTipoUsuario(TipoUsuario.CLIENTE);
        usuario.setAtivo(true);

        usuario.setSenha(
                passwordEncoder.encode(usuario.getSenha())
        );

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario cadastrarFuncionario(
            Usuario usuario,
            TipoUsuario tipoUsuario
    ) {
        if (tipoUsuario == TipoUsuario.CLIENTE) {
            throw new RegraNegocioException(
                    "Selecione FUNCIONARIO ou GERENTE."
            );
        }

        String email =
                usuario.getEmail().trim().toLowerCase();

        usuario.setEmail(email);

        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraNegocioException(
                    "Já existe um usuário cadastrado com esse e-mail."
            );
        }

        usuario.setTipoUsuario(tipoUsuario);
        usuario.setAtivo(true);

        usuario.setSenha(
                passwordEncoder.encode(usuario.getSenha())
        );

        return usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "usuário",
                                id
                        )
                );
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new RegraNegocioException(
                    "E-mail não pode estar vazio."
            );
        }

        return usuarioRepository
                .findByEmail(
                        email.trim().toLowerCase()
                )
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Usuário não encontrado."
                        )
                );
    }

    @Transactional(readOnly = true)
    public String nomePorEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }

        return usuarioRepository
                .findByEmail(
                        email.trim().toLowerCase()
                )
                .map(Usuario::getNome)
                .orElse("");
    }

    @Transactional
    public void alterarStatus(
            Long id,
            boolean ativo
    ) {
        Usuario usuario = buscarPorId(id);
        usuario.setAtivo(ativo);

        usuarioRepository.save(usuario);
    }
}