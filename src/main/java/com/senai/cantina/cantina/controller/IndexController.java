package com.senai.cantina.cantina.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/inicio")
    public String redirecionarAposLogin(
            Authentication authentication
    ) {
        if (possuiPermissao(
                authentication,
                "ROLE_GERENTE"
        )) {
            return "redirect:/gerente";
        }

        if (possuiPermissao(
                authentication,
                "ROLE_FUNCIONARIO"
        )) {
            return "redirect:/funcionario";
        }

        return "redirect:/cardapio";
    }

    private boolean possuiPermissao(
            Authentication authentication,
            String permissao
    ) {
        return authentication
                .getAuthorities()
                .stream()
                .anyMatch(autoridade ->
                        autoridade
                                .getAuthority()
                                .equals(permissao)
                );
    }
}