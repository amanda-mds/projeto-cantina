package com.senai.cantina.cantina.config;

import com.senai.cantina.cantina.model.Usuario;
import com.senai.cantina.cantina.model.Usuario.TipoUsuario;
import com.senai.cantina.cantina.repository.UsuarioRepository;
import com.senai.cantina.cantina.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminInitializer {

    @Bean
    public CommandLineRunner criarGerenteInicial(
            UsuarioRepository usuarioRepository,
            UsuarioService usuarioService,
            @Value("${app.admin.nome}") String nome,
            @Value("${app.admin.email}") String email,
            @Value("${app.admin.telefone}") String telefone,
            @Value("${app.admin.senha}") String senha
    ) {
        return args -> {
            if (senha == null || senha.isBlank()) {
                return;
            }

            String emailNormalizado = email.trim().toLowerCase();

            if (!usuarioRepository.existsByEmail(emailNormalizado)) {
                Usuario gerente = new Usuario();

                gerente.setNome(nome);
                gerente.setEmail(emailNormalizado);
                gerente.setTelefone(telefone);
                gerente.setSenha(senha);

                usuarioService.cadastrarFuncionario(
                        gerente,
                        TipoUsuario.GERENTE
                );
            }
        };
    }
}
