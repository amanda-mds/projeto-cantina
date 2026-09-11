package com.senai.cantina.cantina.service;

import com.senai.cantina.cantina.exception.RegraNegocioException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.senai.cantina.cantina.exception.RecursoNaoEncontradoException;
import com.senai.cantina.cantina.model.ItemPedido;
import com.senai.cantina.cantina.model.Pagamento;
import com.senai.cantina.cantina.model.Pagamento.FormaPagamento;
import com.senai.cantina.cantina.model.Pagamento.StatusPagamento;
import com.senai.cantina.cantina.model.Pedido;
import com.senai.cantina.cantina.model.Pedido.StatusPedido;
import com.senai.cantina.cantina.model.Pedido.TipoPedido;
import com.senai.cantina.cantina.model.Produto;
import com.senai.cantina.cantina.model.MovimentacaoEstoque.MotivoMovimentacao;
import com.senai.cantina.cantina.repository.PagamentoRepository;
import com.senai.cantina.cantina.repository.PedidoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final PedidoRepository pedidoRepository;
    private final MovimentacaoEstoqueService movimentacaoEstoqueService;

    public PagamentoService(
            PagamentoRepository pagamentoRepository,
            PedidoRepository pedidoRepository,
            MovimentacaoEstoqueService movimentacaoEstoqueService
    ) {
        this.pagamentoRepository = pagamentoRepository;
        this.pedidoRepository = pedidoRepository;
        this.movimentacaoEstoqueService =
                movimentacaoEstoqueService;
    }

    @Transactional
    public Pagamento iniciarPagamento(
            Long pedidoId,
            FormaPagamento formaPagamento
    ) {
        Pedido pedido = buscarPedido(pedidoId);

        if (pedido.getStatus()
                != StatusPedido.AGUARDANDO_PAGAMENTO) {

            throw new RegraNegocioException(
                    "Este pedido não está aguardando pagamento."
            );
        }

        if (pedido.getItens().isEmpty()) {
            throw new RegraNegocioException(
                    "O pedido precisa ter pelo menos um item."
            );
        }

        if (pedido.getValorTotal() == null
                || pedido.getValorTotal().signum() <= 0) {

            throw new RegraNegocioException(
                    "O valor do pedido deve ser maior que zero."
            );
        }

        validarFormaPagamento(
                pedido,
                formaPagamento
        );

        Pagamento pagamento =
                pagamentoRepository
                        .findByPedidoId(pedidoId)
                        .orElse(new Pagamento());

        if (pagamento.getId() != null
                && pagamento.getStatus()
                == StatusPagamento.APROVADO) {

            throw new RegraNegocioException(
                    "Este pedido já possui um pagamento aprovado."
            );
        }

        if (pagamento.getId() != null
                && pagamento.getStatus()
                == StatusPagamento.PENDENTE) {

            throw new RegraNegocioException(
                    "Este pedido já possui um pagamento pendente."
            );
        }
        if (pagamento.getId() != null) {
            pagamento.setCodigoTransacao(null);
            pagamentoRepository.save(pagamento);
        }

        pagamento.setPedido(pedido);
        pagamento.setFormaPagamento(
                formaPagamento
        );
        pagamento.setStatus(
                StatusPagamento.PENDENTE
        );
        pagamento.setValor(
                pedido.getValorTotal()
        );
        pagamento.setDataHoraPagamento(null);
        pagamento.setCodigoTransacao(
                gerarCodigoTransacao()
        );

        return pagamentoRepository.save(
                pagamento
        );
    }

    @Transactional
    public Pagamento aprovar(Long pagamentoId) {
        Pagamento pagamento =
                buscarPorId(pagamentoId);

        if (pagamento.getStatus()
                != StatusPagamento.PENDENTE) {

            throw new RegraNegocioException(
                    "Somente pagamentos pendentes podem ser aprovados."
            );
        }

        Pedido pedido = buscarPedido(
                pagamento.getPedido().getId()
        );

        if (pedido.getStatus()
                != StatusPedido.AGUARDANDO_PAGAMENTO) {

            throw new RegraNegocioException(
                    "O pedido não está aguardando pagamento."
            );
        }

        validarEstoque(pedido);

        for (ItemPedido item : pedido.getItens()) {
            movimentacaoEstoqueService
                    .registrarSaida(
                            item.getProduto().getId(),
                            item.getQuantidade(),
                            MotivoMovimentacao.VENDA,
                            "Venda do pedido "
                                    + pedido.getId()
                    );
        }

        pagamento.setStatus(
                StatusPagamento.APROVADO
        );
        pagamento.setDataHoraPagamento(
                LocalDateTime.now()
        );

        pedido.setStatus(StatusPedido.PAGO);
        pedido.setCodigoRetirada(
                gerarCodigoRetirada()
        );

        pedidoRepository.save(pedido);

        return pagamentoRepository.save(
                pagamento
        );
    }

    @Transactional
    public Pagamento recusar(Long pagamentoId) {
        Pagamento pagamento =
                buscarPorId(pagamentoId);

        if (pagamento.getStatus()
                != StatusPagamento.PENDENTE) {

            throw new RegraNegocioException(
                    "Somente pagamentos pendentes podem ser recusados."
            );
        }

        pagamento.setStatus(
                StatusPagamento.RECUSADO
        );

        return pagamentoRepository.save(
                pagamento
        );
    }

    @Transactional
    public Pagamento cancelarPendente(
            Long pagamentoId
    ) {
        Pagamento pagamento =
                buscarPorId(pagamentoId);

        if (pagamento.getStatus()
                != StatusPagamento.PENDENTE) {

            throw new RegraNegocioException(
                    "Somente pagamentos pendentes podem ser cancelados."
            );
        }

        Pedido pedido = buscarPedido(
                pagamento.getPedido().getId()
        );

        if (pedido.getStatus()
                != StatusPedido.AGUARDANDO_PAGAMENTO) {

            throw new RegraNegocioException(
                    "O pedido não pode ser cancelado. Status atual: "
                            + pedido.getStatus().getDescricao()
                            + "."
            );
        }

        pagamento.setStatus(
                StatusPagamento.CANCELADO
        );

        pedido.setStatus(
                StatusPedido.CANCELADO
        );

        pedidoRepository.save(pedido);

        return pagamentoRepository.save(
                pagamento
        );
    }

    @Transactional
    public Pagamento estornar(Long pagamentoId) {
        Pagamento pagamento =
                buscarPorId(pagamentoId);

        if (pagamento.getStatus()
                != StatusPagamento.APROVADO) {

            throw new RegraNegocioException(
                    "Somente pagamentos aprovados podem ser estornados."
            );
        }

        Pedido pedido = buscarPedido(
                pagamento.getPedido().getId()
        );

        if (pedido.getStatus()
                != StatusPedido.PAGO) {

            throw new RegraNegocioException(
                    "O pedido não pode ser estornado após o início do preparo."
            );
        }

        for (ItemPedido item : pedido.getItens()) {
            movimentacaoEstoqueService
                    .registrarEntrada(
                            item.getProduto().getId(),
                            item.getQuantidade(),
                            MotivoMovimentacao.CANCELAMENTO,
                            "Estorno do pedido "
                                    + pedido.getId()
                    );
        }

        pagamento.setStatus(
                StatusPagamento.ESTORNADO
        );

        pedido.setStatus(
                StatusPedido.CANCELADO
        );
        pedido.setCodigoRetirada(null);

        pedidoRepository.save(pedido);

        return pagamentoRepository.save(
                pagamento
        );
    }

    @Transactional(readOnly = true)
    public Pagamento buscarPorId(Long id) {
        return pagamentoRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "pagamento",
                                id
                        )
                );
    }

    @Transactional(readOnly = true)
    public Pagamento buscarPorPedido(
            Long pedidoId
    ) {
        return pagamentoRepository
                .findByPedidoId(pedidoId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Pagamento não encontrado para o pedido informado."
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<Pagamento> listarTodos() {
        return pagamentoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Pagamento> listarPorStatus(
            StatusPagamento status
    ) {
        if (status == null) {
            return pagamentoRepository.findAll();
        }

        return pagamentoRepository
                .findByStatus(status);
    }

    private Pedido buscarPedido(Long id) {
        return pedidoRepository
                .buscarCompletoPorId(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "pedido",
                                id
                        )
                );
    }

    private void validarEstoque(Pedido pedido) {
        for (ItemPedido item : pedido.getItens()) {
            Produto produto = item.getProduto();

            if (!produto.isAtivo()) {
                throw new RegraNegocioException(
                        "O produto "
                                + produto.getNome()
                                + " não está disponível."
                );
            }

            if (produto.getEstoqueAtual()
                    < item.getQuantidade()) {

                throw new RegraNegocioException(
                        "Estoque insuficiente para o produto "
                                + produto.getNome()
                                + ". Disponível: "
                                + produto.getEstoqueAtual()
                                + "."
                );
            }
        }
    }

    private void validarFormaPagamento(
            Pedido pedido,
            FormaPagamento formaPagamento
    ) {
        if (formaPagamento == null) {
            throw new RegraNegocioException(
                    "Informe a forma de pagamento."
            );
        }

        if (pedido.getTipoPedido()
                == TipoPedido.RESERVA
                && formaPagamento
                != FormaPagamento.PIX) {

            throw new RegraNegocioException(
                    "Reservas pelo aplicativo devem ser pagas por Pix."
            );
        }

        if (pedido.getTipoPedido()
                == TipoPedido.PRESENCIAL
                && formaPagamento
                == FormaPagamento.PIX) {

            throw new RegraNegocioException(
                    "Vendas presenciais devem ser pagas com cartão ou dinheiro."
            );
        }
    }

    private String gerarCodigoTransacao() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase();
    }

    private String gerarCodigoRetirada() {
        return UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }
}
