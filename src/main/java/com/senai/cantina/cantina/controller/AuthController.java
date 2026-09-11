package com.senai.cantina.cantina.controller;

import com.senai.cantina.cantina.model.Usuario;
import com.senai.cantina.cantina.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(
            UsuarioService usuarioService
    ) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/cadastro")
    public String exibirCadastro(Model model) {
        model.addAttribute(
                "usuario",
                new Usuario()
        );

        return "cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(
            @Valid
            @ModelAttribute("usuario")
            Usuario usuario,
            BindingResult resultado,
            Model model,
            RedirectAttributes flash
    ) {
        if (resultado.hasErrors()) {
            return "cadastro";
        }

        try {
            usuarioService.cadastrarCliente(
                    usuario
            );

            flash.addFlashAttribute(
                    "sucesso",
                    "Cadastro realizado com sucesso! Faça o login para continuar."
            );

            return "redirect:/login";

        } catch (Exception exception) {
            model.addAttribute(
                    "erro",
                    exception.getMessage()
            );

            return "cadastro";
        }
    }
}