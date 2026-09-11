package com.senai.cantina.cantina.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.senai.cantina.cantina.model.Pagamento;
import com.senai.cantina.cantina.model.Pagamento.FormaPagamento;
import com.senai.cantina.cantina.model.Pedido;
import com.senai.cantina.cantina.model.Pedido.TipoPedido;
import com.senai.cantina.cantina.service.PagamentoService;
import com.senai.cantina.cantina.service.PedidoService;

@Controller
@RequestMapping("/pagamento")
public class PagamentoController {

    private final PagamentoService pagamentoService;
    private final PedidoService pedidoService;

    public PagamentoController(
            PagamentoService pagamentoService,
            PedidoService pedidoService
    ) {
        this.pagamentoService = pagamentoService;
        this.pedidoService = pedidoService;
    }

    @GetMapping("/pedido/{pedidoId}")
    public String exibirPagamento(
            @PathVariable Long pedidoId,
            Authentication authentication,
            Model model
    ) {
        Pedido pedido
                = pedidoService.buscarPorId(
                        pedidoId
                );

        verificarAcesso(
                pedido,
                authentication
        );

        model.addAttribute(
                "pedido",
                pedido
        );

        model.addAttribute(
                "formasPagamento",
                obterFormasPagamento(pedido)
        );

        return "pagamento/formulario";
    }

    @PostMapping("/pedido/{pedidoId}")
    public String iniciarPagamento(
            @PathVariable Long pedidoId,
            @RequestParam FormaPagamento formaPagamento,
            Authentication authentication,
            RedirectAttributes flash
    ) {
        try {
            Pedido pedido
                    = pedidoService.buscarPorId(
                            pedidoId
                    );

            verificarAcesso(
                    pedido,
                    authentication
            );

            Pagamento pagamento
                    = pagamentoService
                            .iniciarPagamento(
                                    pedidoId,
                                    formaPagamento
                            );

            flash.addFlashAttribute(
                    "sucesso",
                    "Pagamento iniciado com sucesso."
            );

            return "redirect:/pagamento/"
                    + pagamento.getId();

        } catch (Exception exception) {
            flash.addFlashAttribute(
                    "erro",
                    exception.getMessage()
            );

            return "redirect:/pagamento/pedido/"
                    + pedidoId;
        }
    }

    @GetMapping("/{id}")
    public String detalhes(
            @PathVariable Long id,
            Authentication authentication,
            Model model
    ) {
        Pagamento pagamento
                = pagamentoService.buscarPorId(id);

        Pedido pedido
                = pedidoService.buscarPorId(
                        pagamento.getPedido().getId()
                );

        verificarAcesso(
                pedido,
                authentication
        );

        model.addAttribute(
                "pagamento",
                pagamento
        );

        model.addAttribute(
                "pedido",
                pedido
        );

        return "pagamento/detalhes";
    }

    @PostMapping("/{id}/aprovar")
    public String aprovar(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes flash
    ) {

        if (!possuiPermissao(authentication, "ROLE_FUNCIONARIO")
                && !possuiPermissao(authentication, "ROLE_GERENTE")) {

            flash.addFlashAttribute(
                    "erro",
                    "Somente funcionários podem aprovar pagamentos."
            );

            return "redirect:/pagamento/" + id;
        }

        try {
            pagamentoService.aprovar(id);

            flash.addFlashAttribute(
                    "sucesso",
                    "Pagamento aprovado! Código de retirada gerado."
            );

        } catch (Exception exception) {
            flash.addFlashAttribute(
                    "erro",
                    exception.getMessage()
            );
        }

        return "redirect:/pagamento/" + id;
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes flash
    ) {
        try {
            Pagamento pagamento
                    = pagamentoService.buscarPorId(id);

            Pedido pedido
                    = pedidoService.buscarPorId(
                            pagamento
                                    .getPedido()
                                    .getId()
                    );

            verificarAcesso(
                    pedido,
                    authentication
            );

            pagamentoService.cancelarPendente(
                    id
            );

            flash.addFlashAttribute(
                    "sucesso",
                    "Pagamento e pedido cancelados."
            );

            return "redirect:/pedido/meus";

        } catch (Exception exception) {
            flash.addFlashAttribute(
                    "erro",
                    exception.getMessage()
            );

            return "redirect:/pagamento/" + id;
        }
    }

    private List<FormaPagamento>
            obterFormasPagamento(Pedido pedido) {

        if (pedido.getTipoPedido()
                == TipoPedido.RESERVA) {

            return List.of(
                    FormaPagamento.PIX
            );
        }

        return List.of(
                FormaPagamento.CREDITO,
                FormaPagamento.DEBITO,
                FormaPagamento.DINHEIRO
        );
    }

    private void verificarAcesso(
            Pedido pedido,
            Authentication authentication
    ) {
        boolean funcionario
                = possuiPermissao(
                        authentication,
                        "ROLE_FUNCIONARIO"
                );

        boolean gerente
                = possuiPermissao(
                        authentication,
                        "ROLE_GERENTE"
                );

        if (funcionario || gerente) {
            return;
        }

        String emailPedido
                = pedido.getUsuario().getEmail();

        String emailLogado
                = authentication.getName();

        if (!emailPedido.equalsIgnoreCase(
                emailLogado
        )) {
            throw new IllegalStateException(
                    "Você não possui permissão para acessar este pagamento."
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
                .anyMatch(autoridade
                        -> autoridade
                        .getAuthority()
                        .equals(permissao)
                );
    }
}
