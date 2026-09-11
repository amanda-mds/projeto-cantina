package com.senai.cantina.cantina.controller;

import java.util.List;

import com.senai.cantina.cantina.model.MovimentacaoEstoque.MotivoMovimentacao;
import com.senai.cantina.cantina.model.Produto;
import com.senai.cantina.cantina.model.Produto.CategoriaProduto;
import com.senai.cantina.cantina.model.Usuario;
import com.senai.cantina.cantina.model.Usuario.TipoUsuario;
import com.senai.cantina.cantina.service.MovimentacaoEstoqueService;
import com.senai.cantina.cantina.service.PedidoService;
import com.senai.cantina.cantina.service.ProdutoService;
import com.senai.cantina.cantina.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/gerente")
public class GerenteController {

    private final ProdutoService produtoService;
    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;
    private final MovimentacaoEstoqueService movimentacaoEstoqueService;

    public GerenteController(
            ProdutoService produtoService,
            PedidoService pedidoService,
            UsuarioService usuarioService,
            MovimentacaoEstoqueService movimentacaoEstoqueService
    ) {
        this.produtoService = produtoService;
        this.pedidoService = pedidoService;
        this.usuarioService = usuarioService;
        this.movimentacaoEstoqueService =
                movimentacaoEstoqueService;
    }

    @GetMapping
    public String painel(Model model) {
        model.addAttribute(
                "quantidadeProdutos",
                produtoService.listarTodos().size()
        );

        model.addAttribute(
                "quantidadePedidos",
                pedidoService.listarTodos().size()
        );

        model.addAttribute(
                "quantidadeUsuarios",
                usuarioService.listarTodos().size()
        );

        model.addAttribute(
                "produtosEstoqueBaixo",
                produtoService
                        .listarComEstoqueBaixo()
        );

        model.addAttribute(
                "perdas",
                movimentacaoEstoqueService
                        .listarPorMotivo(
                                MotivoMovimentacao.PERDA
                        )
        );

        return "gerente/painel";
    }

    @GetMapping("/produtos")
    public String listarProdutos(Model model) {
        model.addAttribute(
                "produtos",
                produtoService.listarTodos()
        );

        return "gerente/produtos/lista";
    }

    @GetMapping("/produtos/novo")
    public String exibirNovoProduto(
            Model model
    ) {
        model.addAttribute(
                "produto",
                new Produto()
        );

        adicionarCategorias(model);

        return "gerente/produtos/novo";
    }

    @PostMapping("/produtos/novo")
    public String cadastrarProduto(
            @Valid
            @ModelAttribute("produto")
            Produto produto,
            BindingResult resultado,
            Model model,
            RedirectAttributes flash
    ) {
        if (resultado.hasErrors()) {
            adicionarCategorias(model);

            return "gerente/produtos/novo";
        }

        try {
            Produto salvo =
                    produtoService.cadastrar(
                            produto
                    );

            flash.addFlashAttribute(
                    "sucesso",
                    "Produto cadastrado com sucesso."
            );

            return "redirect:/gerente/estoque/"
                    + salvo.getId();

        } catch (Exception exception) {
            model.addAttribute(
                    "erro",
                    exception.getMessage()
            );

            adicionarCategorias(model);

            return "gerente/produtos/novo";
        }
    }

    @GetMapping("/produtos/{id}/editar")
    public String exibirEdicaoProduto(
            @PathVariable Long id,
            Model model
    ) {
        model.addAttribute(
                "produto",
                produtoService.buscarPorId(id)
        );

        adicionarCategorias(model);

        return "gerente/produtos/editar";
    }

    @PostMapping("/produtos/{id}/editar")
    public String atualizarProduto(
            @PathVariable Long id,
            @Valid
            @ModelAttribute("produto")
            Produto produto,
            BindingResult resultado,
            Model model,
            RedirectAttributes flash
    ) {
        if (resultado.hasErrors()) {
            produto.setId(id);
            adicionarCategorias(model);

            return "gerente/produtos/editar";
        }

        try {
            produtoService.atualizar(
                    id,
                    produto
            );

            flash.addFlashAttribute(
                    "sucesso",
                    "Produto atualizado com sucesso."
            );

            return "redirect:/gerente/produtos";

        } catch (Exception exception) {
            produto.setId(id);

            model.addAttribute(
                    "erro",
                    exception.getMessage()
            );

            adicionarCategorias(model);

            return "gerente/produtos/editar";
        }
    }

    @PostMapping("/produtos/{id}/status")
    public String alterarStatusProduto(
            @PathVariable Long id,
            @RequestParam boolean ativo,
            RedirectAttributes flash
    ) {
        try {
            produtoService.alterarStatus(
                    id,
                    ativo
            );

            flash.addFlashAttribute(
                    "sucesso",
                    "Status do produto atualizado."
            );

        } catch (Exception exception) {
            flash.addFlashAttribute(
                    "erro",
                    exception.getMessage()
            );
        }

        return "redirect:/gerente/produtos";
    }

    @GetMapping("/estoque/{produtoId}")
    public String exibirEstoque(
            @PathVariable Long produtoId,
            Model model
    ) {
        model.addAttribute(
                "produto",
                produtoService.buscarPorId(
                        produtoId
                )
        );

        model.addAttribute(
                "movimentacoes",
                movimentacaoEstoqueService
                        .listarPorProduto(
                                produtoId
                        )
        );

        return "gerente/estoque/detalhes";
    }

    @PostMapping("/estoque/{produtoId}/entrada")
    public String registrarEntrada(
            @PathVariable Long produtoId,
            @RequestParam int quantidade,
            @RequestParam(required = false)
            String observacao,
            RedirectAttributes flash
    ) {
        try {
            movimentacaoEstoqueService
                    .registrarEntrada(
                            produtoId,
                            quantidade,
                            MotivoMovimentacao.COMPRA,
                            observacao
                    );

            flash.addFlashAttribute(
                    "sucesso",
                    "Entrada registrada com sucesso."
            );

        } catch (Exception exception) {
            flash.addFlashAttribute(
                    "erro",
                    exception.getMessage()
            );
        }

        return "redirect:/gerente/estoque/"
                + produtoId;
    }

    @PostMapping("/estoque/{produtoId}/perda")
    public String registrarPerda(
            @PathVariable Long produtoId,
            @RequestParam int quantidade,
            @RequestParam(required = false)
            String observacao,
            RedirectAttributes flash
    ) {
        try {
            movimentacaoEstoqueService
                    .registrarPerda(
                            produtoId,
                            quantidade,
                            observacao
                    );

            flash.addFlashAttribute(
                    "sucesso",
                    "Perda registrada com sucesso."
            );

        } catch (Exception exception) {
            flash.addFlashAttribute(
                    "erro",
                    exception.getMessage()
            );
        }

        return "redirect:/gerente/estoque/"
                + produtoId;
    }

    @PostMapping("/estoque/{produtoId}/ajuste")
    public String ajustarEstoque(
            @PathVariable Long produtoId,
            @RequestParam int novoSaldo,
            @RequestParam(required = false)
            String observacao,
            RedirectAttributes flash
    ) {
        try {
            movimentacaoEstoqueService
                    .ajustarEstoque(
                            produtoId,
                            novoSaldo,
                            observacao
                    );

            flash.addFlashAttribute(
                    "sucesso",
                    "Estoque ajustado com sucesso."
            );

        } catch (Exception exception) {
            flash.addFlashAttribute(
                    "erro",
                    exception.getMessage()
            );
        }

        return "redirect:/gerente/estoque/"
                + produtoId;
    }

    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        model.addAttribute(
                "usuarios",
                usuarioService.listarTodos()
        );

        return "gerente/usuarios/lista";
    }

    @GetMapping("/usuarios/novo")
    public String exibirNovoFuncionario(
            Model model
    ) {
        model.addAttribute(
                "usuario",
                new Usuario()
        );

        adicionarTiposFuncionario(model);

        return "gerente/usuarios/novo";
    }

    @PostMapping("/usuarios/novo")
    public String cadastrarFuncionario(
            @Valid
            @ModelAttribute("usuario")
            Usuario usuario,
            BindingResult resultado,
            @RequestParam
            TipoUsuario tipoUsuario,
            Model model,
            RedirectAttributes flash
    ) {
        if (resultado.hasErrors()) {
            adicionarTiposFuncionario(model);

            return "gerente/usuarios/novo";
        }

        try {
            usuarioService
                    .cadastrarFuncionario(
                            usuario,
                            tipoUsuario
                    );

            flash.addFlashAttribute(
                    "sucesso",
                    "Funcionário cadastrado com sucesso."
            );

            return "redirect:/gerente/usuarios";

        } catch (Exception exception) {
            model.addAttribute(
                    "erro",
                    exception.getMessage()
            );

            adicionarTiposFuncionario(model);

            return "gerente/usuarios/novo";
        }
    }

    @PostMapping("/usuarios/{id}/status")
    public String alterarStatusUsuario(
            @PathVariable Long id,
            @RequestParam boolean ativo,
            RedirectAttributes flash
    ) {
        try {
            usuarioService.alterarStatus(
                    id,
                    ativo
            );

            flash.addFlashAttribute(
                    "sucesso",
                    "Status do usuário atualizado."
            );

        } catch (Exception exception) {
            flash.addFlashAttribute(
                    "erro",
                    exception.getMessage()
            );
        }

        return "redirect:/gerente/usuarios";
    }

    private void adicionarCategorias(
            Model model
    ) {
        model.addAttribute(
                "categorias",
                CategoriaProduto.values()
        );
    }

    private void adicionarTiposFuncionario(
            Model model
    ) {
        model.addAttribute(
                "tiposFuncionario",
                List.of(
                        TipoUsuario.FUNCIONARIO,
                        TipoUsuario.GERENTE
                )
        );
    }
}