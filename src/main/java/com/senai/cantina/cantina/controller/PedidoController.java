package com.senai.cantina.cantina.controller;

import java.security.Principal;

import com.senai.cantina.cantina.model.Pedido;
import com.senai.cantina.cantina.model.Pedido.TipoPedido;
import com.senai.cantina.cantina.model.Usuario;
import com.senai.cantina.cantina.service.PedidoService;
import com.senai.cantina.cantina.service.ProdutoService;
import com.senai.cantina.cantina.service.UsuarioService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pedido")
public class PedidoController {

    private final PedidoService pedidoService;
    private final ProdutoService produtoService;
    private final UsuarioService usuarioService;

    public PedidoController(
            PedidoService pedidoService,
            ProdutoService produtoService,
            UsuarioService usuarioService
    ) {
        this.pedidoService = pedidoService;
        this.produtoService = produtoService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/novo")
    public String exibirNovoPedido(
            Model model
    ) {
        Pedido pedido = new Pedido();

        pedido.setTipoPedido(
                TipoPedido.RESERVA
        );

        model.addAttribute(
                "pedido",
                pedido
        );

        model.addAttribute(
                "tiposPedido",
                TipoPedido.values()
        );

        return "pedido/novo";
    }

    @PostMapping("/novo")
    public String criarPedido(
            @ModelAttribute("pedido")
            Pedido pedido,
            Principal principal,
            Model model,
            RedirectAttributes flash
    ) {
        try {
            Usuario usuario =
                    usuarioService.buscarPorEmail(
                            principal.getName()
                    );

            Pedido salvo =
                    pedidoService.criar(
                            usuario.getId(),
                            pedido
                    );

            flash.addFlashAttribute(
                    "sucesso",
                    "Pedido iniciado com sucesso! Agora adicione os produtos."
            );

            return "redirect:/pedido/"
                    + salvo.getId();

        } catch (Exception exception) {
            model.addAttribute(
                    "erro",
                    exception.getMessage()
            );

            model.addAttribute(
                    "tiposPedido",
                    TipoPedido.values()
            );

            return "pedido/novo";
        }
    }

    @GetMapping("/meus")
    public String meusPedidos(
            Principal principal,
            Model model
    ) {
        Usuario usuario =
                usuarioService.buscarPorEmail(
                        principal.getName()
                );

        model.addAttribute(
                "pedidos",
                pedidoService.listarPorUsuario(
                        usuario.getId()
                )
        );

        return "pedido/meus-pedidos";
    }

    @GetMapping("/{id}")
    public String detalhes(
            @PathVariable Long id,
            Authentication authentication,
            Model model
    ) {
        Pedido pedido =
                pedidoService.buscarPorId(id);

        verificarAcesso(
                pedido,
                authentication
        );

        model.addAttribute(
                "pedido",
                pedido
        );

        model.addAttribute(
                "produtos",
                produtoService.listarAtivos()
        );

        return "pedido/detalhes";
    }

    @PostMapping("/{id}/item/adicionar")
    public String adicionarItem(
            @PathVariable Long id,
            @RequestParam Long produtoId,
            @RequestParam int quantidade,
            Authentication authentication,
            RedirectAttributes flash
    ) {
        try {
            Pedido pedido =
                    pedidoService.buscarPorId(id);

            verificarAcesso(
                    pedido,
                    authentication
            );

            pedidoService.adicionarItem(
                    id,
                    produtoId,
                    quantidade
            );

            flash.addFlashAttribute(
                    "sucesso",
                    "Produto adicionado ao pedido."
            );

        } catch (Exception exception) {
            flash.addFlashAttribute(
                    "erro",
                    exception.getMessage()
            );
        }

        return "redirect:/pedido/" + id;
    }

    @PostMapping(
            "/{id}/item/{itemId}/quantidade"
    )
    public String alterarQuantidade(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @RequestParam int quantidade,
            Authentication authentication,
            RedirectAttributes flash
    ) {
        try {
            Pedido pedido =
                    pedidoService.buscarPorId(id);

            verificarAcesso(
                    pedido,
                    authentication
            );

            pedidoService.alterarQuantidade(
                    id,
                    itemId,
                    quantidade
            );

            flash.addFlashAttribute(
                    "sucesso",
                    "Quantidade atualizada."
            );

        } catch (Exception exception) {
            flash.addFlashAttribute(
                    "erro",
                    exception.getMessage()
            );
        }

        return "redirect:/pedido/" + id;
    }

    @PostMapping(
            "/{id}/item/{itemId}/remover"
    )
    public String removerItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            Authentication authentication,
            RedirectAttributes flash
    ) {
        try {
            Pedido pedido =
                    pedidoService.buscarPorId(id);

            verificarAcesso(
                    pedido,
                    authentication
            );

            pedidoService.removerItem(
                    id,
                    itemId
            );

            flash.addFlashAttribute(
                    "sucesso",
                    "Produto removido do pedido."
            );

        } catch (Exception exception) {
            flash.addFlashAttribute(
                    "erro",
                    exception.getMessage()
            );
        }

        return "redirect:/pedido/" + id;
    }

    @PostMapping("/{id}/confirmar")
    public String confirmarPedido(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes flash
    ) {
        try {
            Pedido pedido =
                    pedidoService.buscarPorId(id);

            verificarAcesso(
                    pedido,
                    authentication
            );

            pedidoService.confirmarPedido(id);

            return "redirect:/pagamento/pedido/"
                    + id;

        } catch (Exception exception) {
            flash.addFlashAttribute(
                    "erro",
                    exception.getMessage()
            );

            return "redirect:/pedido/" + id;
        }
    }

    @PostMapping("/{id}/cancelar")
    public String cancelarPedido(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes flash
    ) {
        try {
            Pedido pedido =
                    pedidoService.buscarPorId(id);

            verificarAcesso(
                    pedido,
                    authentication
            );

            pedidoService.cancelarPendente(id);

            flash.addFlashAttribute(
                    "sucesso",
                    "Pedido cancelado com sucesso."
            );

            return "redirect:/pedido/meus";

        } catch (Exception exception) {
            flash.addFlashAttribute(
                    "erro",
                    exception.getMessage()
            );

            return "redirect:/pedido/" + id;
        }
    }

    private void verificarAcesso(
            Pedido pedido,
            Authentication authentication
    ) {
        boolean funcionario =
                possuiPermissao(
                        authentication,
                        "ROLE_FUNCIONARIO"
                );

        boolean gerente =
                possuiPermissao(
                        authentication,
                        "ROLE_GERENTE"
                );

        if (funcionario || gerente) {
            return;
        }

        String emailPedido =
                pedido.getUsuario().getEmail();

        String emailLogado =
                authentication.getName();

        if (!emailPedido.equalsIgnoreCase(
                emailLogado
        )) {
            throw new IllegalStateException(
                    "Você não possui permissão para acessar este pedido."
            );
        }
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