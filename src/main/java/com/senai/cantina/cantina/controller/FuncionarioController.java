package com.senai.cantina.cantina.controller;

import com.senai.cantina.cantina.model.Pedido;
import com.senai.cantina.cantina.model.Pedido.StatusPedido;
import com.senai.cantina.cantina.model.Pedido.TipoPedido;
import com.senai.cantina.cantina.service.PagamentoService;
import com.senai.cantina.cantina.service.PedidoService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/funcionario")
public class FuncionarioController {

    private final PedidoService pedidoService;
    private final PagamentoService pagamentoService;

    public FuncionarioController(
            PedidoService pedidoService,
            PagamentoService pagamentoService
    ) {
        this.pedidoService = pedidoService;
        this.pagamentoService = pagamentoService;
    }

    @GetMapping
    public String painel(
            @RequestParam(required = false)
            StatusPedido status,
            @RequestParam(required = false)
            TipoPedido tipoPedido,
            Model model
    ) {
        model.addAttribute(
                "pedidos",
                pedidoService.filtrar(
                        status,
                        tipoPedido
                )
        );

        model.addAttribute(
                "statusList",
                StatusPedido.values()
        );

        model.addAttribute(
                "tiposPedido",
                TipoPedido.values()
        );

        model.addAttribute(
                "filtroStatus",
                status
        );

        model.addAttribute(
                "filtroTipo",
                tipoPedido
        );

        return "funcionario/painel";
    }

    @GetMapping("/pedido/{id}")
    public String detalhesPedido(
            @PathVariable Long id,
            Model model
    ) {
        Pedido pedido =
                pedidoService.buscarPorId(id);

        model.addAttribute(
                "pedido",
                pedido
        );

        try {
            model.addAttribute(
                    "pagamento",
                    pagamentoService.buscarPorPedido(id)
            );
        } catch (Exception exception) {
            model.addAttribute(
                    "pagamento",
                    null
            );
        }

        return "funcionario/detalhes-pedido";
    }

    @PostMapping("/pedido/{id}/preparar")
    public String iniciarPreparo(
            @PathVariable Long id,
            RedirectAttributes flash
    ) {
        try {
            pedidoService.iniciarPreparo(id);

            flash.addFlashAttribute(
                    "sucesso",
                    "Preparo iniciado com sucesso."
            );

        } catch (Exception exception) {
            flash.addFlashAttribute(
                    "erro",
                    exception.getMessage()
            );
        }

        return "redirect:/funcionario/pedido/"
                + id;
    }

    @PostMapping("/pedido/{id}/pronto")
    public String marcarComoPronto(
            @PathVariable Long id,
            RedirectAttributes flash
    ) {
        try {
            pedidoService.marcarComoPronto(id);

            flash.addFlashAttribute(
                    "sucesso",
                    "Pedido marcado como pronto."
            );

        } catch (Exception exception) {
            flash.addFlashAttribute(
                    "erro",
                    exception.getMessage()
            );
        }

        return "redirect:/funcionario/pedido/"
                + id;
    }

    @PostMapping("/pedido/{id}/retirar")
    public String marcarComoRetirado(
            @PathVariable Long id,
            RedirectAttributes flash
    ) {
        try {
            pedidoService.marcarComoRetirado(id);

            flash.addFlashAttribute(
                    "sucesso",
                    "Retirada confirmada com sucesso."
            );

        } catch (Exception exception) {
            flash.addFlashAttribute(
                    "erro",
                    exception.getMessage()
            );
        }

        return "redirect:/funcionario/pedido/"
                + id;
    }

    @GetMapping("/verificar")
    public String verificarRetirada(
            @RequestParam(required = false)
            String codigo,
            Model model
    ) {
        if (codigo != null
                && !codigo.isBlank()) {

            try {
                Pedido pedido =
                        pedidoService
                                .buscarPorCodigoRetirada(
                                        codigo
                                );

                model.addAttribute(
                        "pedido",
                        pedido
                );

            } catch (Exception exception) {
                model.addAttribute(
                        "erro",
                        exception.getMessage()
                );
            }
        }

        model.addAttribute(
                "codigoInformado",
                codigo
        );

        return "funcionario/verificar-retirada";
    }

    @PostMapping("/verificar/{id}/retirar")
    public String confirmarRetiradaPorCodigo(
            @PathVariable Long id,
            RedirectAttributes flash
    ) {
        try {
            pedidoService.marcarComoRetirado(id);

            flash.addFlashAttribute(
                    "sucesso",
                    "Retirada confirmada com sucesso."
            );

            return "redirect:/funcionario";

        } catch (Exception exception) {
            flash.addFlashAttribute(
                    "erro",
                    exception.getMessage()
            );

            return "redirect:/funcionario/pedido/"
                    + id;
        }
    }
}