package com.senai.cantina.cantina.controller;

import com.senai.cantina.cantina.model.Produto;
import com.senai.cantina.cantina.model.Produto.CategoriaProduto;
import com.senai.cantina.cantina.service.ProdutoService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/cardapio")
public class CardapioController {

    private final ProdutoService produtoService;

    public CardapioController(
            ProdutoService produtoService
    ) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public String listar(
            @RequestParam(required = false)
            CategoriaProduto categoria,
            @RequestParam(required = false)
            String nome,
            Model model
    ) {
        model.addAttribute(
                "produtos",
                produtoService.filtrar(
                        categoria,
                        nome
                )
        );

        model.addAttribute(
                "categorias",
                CategoriaProduto.values()
        );

        model.addAttribute(
                "filtroCategoria",
                categoria
        );

        model.addAttribute(
                "filtroNome",
                nome
        );

        return "cardapio/lista";
    }

    @GetMapping("/{id}")
    public String detalhes(
            @PathVariable Long id,
            Model model
    ) {
        Produto produto =
                produtoService.buscarPorId(id);

        if (!produto.isAtivo()) {
            return "redirect:/cardapio";
        }

        model.addAttribute(
                "produto",
                produto
        );

        return "cardapio/detalhes";
    }
}