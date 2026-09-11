package com.senai.cantina.cantina.service;

import com.senai.cantina.cantina.exception.RegraNegocioException;
import com.senai.cantina.cantina.model.ItemPedido;
import com.senai.cantina.cantina.model.MovimentacaoEstoque.MotivoMovimentacao;
import com.senai.cantina.cantina.model.Pagamento;
import com.senai.cantina.cantina.model.Pagamento.FormaPagamento;
import com.senai.cantina.cantina.model.Pagamento.StatusPagamento;
import com.senai.cantina.cantina.model.Pedido;
import com.senai.cantina.cantina.model.Pedido.StatusPedido;
import com.senai.cantina.cantina.model.Pedido.TipoPedido;
import com.senai.cantina.cantina.model.Produto;
import com.senai.cantina.cantina.repository.PagamentoRepository;
import com.senai.cantina.cantina.repository.PedidoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagamentoService")
class PagamentoServiceTest {

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private MovimentacaoEstoqueService movimentacaoEstoqueService;

    @InjectMocks
    private PagamentoService pagamentoService;

    private Produto produto;
    private Pedido pedidoReserva;
    private Pedido pedidoPresencial;

    @BeforeEach
    void configurarCenarioBase() {
        produto = new Produto();
        produto.setId(10L);
        produto.setNome("Coxinha");
        produto.setPrecoVenda(new BigDecimal("8.00"));
        produto.setEstoqueAtual(5);
        produto.setAtivo(true);

        pedidoReserva = criarPedidoComItem(TipoPedido.RESERVA);
        pedidoPresencial = criarPedidoComItem(TipoPedido.PRESENCIAL);
    }

    @Nested
    @DisplayName("iniciarPagamento()")
    class IniciarPagamento {

        @Test
        @DisplayName("reserva pelo aplicativo só pode ser paga via Pix")
        void reservaSoPodeSerPagaViaPix() {
            when(pedidoRepository.buscarCompletoPorId(pedidoReserva.getId()))
                    .thenReturn(Optional.of(pedidoReserva));

            assertThatThrownBy(() ->
                    pagamentoService.iniciarPagamento(
                            pedidoReserva.getId(),
                            FormaPagamento.CREDITO
                    )
            )
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("devem ser pagas por Pix");
        }

        @Test
        @DisplayName("venda presencial não pode ser paga via Pix")
        void vendaPresencialNaoPodeSerPagaViaPix() {
            when(pedidoRepository.buscarCompletoPorId(pedidoPresencial.getId()))
                    .thenReturn(Optional.of(pedidoPresencial));

            assertThatThrownBy(() ->
                    pagamentoService.iniciarPagamento(
                            pedidoPresencial.getId(),
                            FormaPagamento.PIX
                    )
            )
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("cartão ou dinheiro");
        }

        @Test
        @DisplayName("deve iniciar pagamento com sucesso quando a forma é compatível com o tipo do pedido")
        void deveIniciarPagamentoComSucesso() {
            when(pedidoRepository.buscarCompletoPorId(pedidoReserva.getId()))
                    .thenReturn(Optional.of(pedidoReserva));
            when(pagamentoRepository.findByPedidoId(pedidoReserva.getId()))
                    .thenReturn(Optional.empty());
            when(pagamentoRepository.save(any(Pagamento.class)))
                    .thenAnswer(chamada -> chamada.getArgument(0));

            Pagamento resultado = pagamentoService.iniciarPagamento(
                    pedidoReserva.getId(),
                    FormaPagamento.PIX
            );

            assertThat(resultado.getStatus())
                    .isEqualTo(StatusPagamento.PENDENTE);
            assertThat(resultado.getValor())
                    .isEqualByComparingTo(pedidoReserva.getValorTotal());
            assertThat(resultado.getCodigoTransacao()).isNotBlank();
        }

        @Test
        @DisplayName("não deve iniciar pagamento para pedido sem itens")
        void naoDeveIniciarPagamentoParaPedidoSemItens() {
            Pedido pedidoVazio = criarPedidoComItem(TipoPedido.RESERVA);
            pedidoVazio.getItens().clear();
            pedidoVazio.setValorTotal(BigDecimal.ZERO);

            when(pedidoRepository.buscarCompletoPorId(pedidoVazio.getId()))
                    .thenReturn(Optional.of(pedidoVazio));

            assertThatThrownBy(() ->
                    pagamentoService.iniciarPagamento(
                            pedidoVazio.getId(),
                            FormaPagamento.PIX
                    )
            )
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("pelo menos um item");
        }

        @Test
        @DisplayName("não deve permitir novo pagamento se já existe um aprovado")
        void naoDevePermitirNovoPagamentoSeJaAprovado() {
            Pagamento pagamentoExistente = new Pagamento();
            pagamentoExistente.setId(1L);
            pagamentoExistente.setStatus(StatusPagamento.APROVADO);

            when(pedidoRepository.buscarCompletoPorId(pedidoReserva.getId()))
                    .thenReturn(Optional.of(pedidoReserva));
            when(pagamentoRepository.findByPedidoId(pedidoReserva.getId()))
                    .thenReturn(Optional.of(pagamentoExistente));

            assertThatThrownBy(() ->
                    pagamentoService.iniciarPagamento(
                            pedidoReserva.getId(),
                            FormaPagamento.PIX
                    )
            )
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("já possui um pagamento aprovado");
        }
    }

    @Nested
    @DisplayName("aprovar()")
    class Aprovar {

        @Test
        @DisplayName("deve aprovar pagamento, dar baixa no estoque e gerar código de retirada")
        void deveAprovarPagamentoComSucesso() {
            Pagamento pagamento = criarPagamentoPendente(pedidoReserva);

            when(pagamentoRepository.findById(pagamento.getId()))
                    .thenReturn(Optional.of(pagamento));
            when(pedidoRepository.buscarCompletoPorId(pedidoReserva.getId()))
                    .thenReturn(Optional.of(pedidoReserva));
            when(pedidoRepository.save(any(Pedido.class)))
                    .thenAnswer(chamada -> chamada.getArgument(0));
            when(pagamentoRepository.save(any(Pagamento.class)))
                    .thenAnswer(chamada -> chamada.getArgument(0));

            Pagamento resultado = pagamentoService.aprovar(pagamento.getId());

            assertThat(resultado.getStatus())
                    .isEqualTo(StatusPagamento.APROVADO);
            assertThat(pedidoReserva.getStatus())
                    .isEqualTo(StatusPedido.PAGO);
            assertThat(pedidoReserva.getCodigoRetirada()).isNotBlank();

            // Confirma que a baixa de estoque foi registrada
            // para o item do pedido.
            verify(movimentacaoEstoqueService).registrarSaida(
                    eq(produto.getId()),
                    eq(2),
                    eq(MotivoMovimentacao.VENDA),
                    anyString()
            );
        }

        @Test
        @DisplayName("não deve aprovar pagamento se o estoque ficou insuficiente entre o pedido e a aprovação")
        void naoDeveAprovarSeEstoqueInsuficiente() {
            produto.setEstoqueAtual(1); // pedido pede 2, só tem 1

            Pagamento pagamento = criarPagamentoPendente(pedidoReserva);

            when(pagamentoRepository.findById(pagamento.getId()))
                    .thenReturn(Optional.of(pagamento));
            when(pedidoRepository.buscarCompletoPorId(pedidoReserva.getId()))
                    .thenReturn(Optional.of(pedidoReserva));

            assertThatThrownBy(() ->
                    pagamentoService.aprovar(pagamento.getId())
            )
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Estoque insuficiente");

            verify(movimentacaoEstoqueService, never())
                    .registrarSaida(any(), anyInt(), any(), anyString());
            verify(pagamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("não deve aprovar pagamento que já não está mais pendente")
        void naoDeveAprovarPagamentoQueNaoEstaPendente() {
            Pagamento pagamento = criarPagamentoPendente(pedidoReserva);
            pagamento.setStatus(StatusPagamento.RECUSADO);

            when(pagamentoRepository.findById(pagamento.getId()))
                    .thenReturn(Optional.of(pagamento));

            assertThatThrownBy(() ->
                    pagamentoService.aprovar(pagamento.getId())
            )
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Somente pagamentos pendentes");
        }
    }

    @Nested
    @DisplayName("estornar()")
    class Estornar {

        @Test
        @DisplayName("deve estornar pagamento aprovado e devolver os itens ao estoque")
        void deveEstornarPagamentoEDevolverEstoque() {
            Pagamento pagamento = criarPagamentoPendente(pedidoReserva);
            pagamento.setStatus(StatusPagamento.APROVADO);
            pedidoReserva.setStatus(StatusPedido.PAGO);

            when(pagamentoRepository.findById(pagamento.getId()))
                    .thenReturn(Optional.of(pagamento));
            when(pedidoRepository.buscarCompletoPorId(pedidoReserva.getId()))
                    .thenReturn(Optional.of(pedidoReserva));
            when(pedidoRepository.save(any(Pedido.class)))
                    .thenAnswer(chamada -> chamada.getArgument(0));
            when(pagamentoRepository.save(any(Pagamento.class)))
                    .thenAnswer(chamada -> chamada.getArgument(0));

            Pagamento resultado = pagamentoService.estornar(pagamento.getId());

            assertThat(resultado.getStatus())
                    .isEqualTo(StatusPagamento.ESTORNADO);
            assertThat(pedidoReserva.getStatus())
                    .isEqualTo(StatusPedido.CANCELADO);
            assertThat(pedidoReserva.getCodigoRetirada()).isNull();

            verify(movimentacaoEstoqueService).registrarEntrada(
                    eq(produto.getId()),
                    eq(2),
                    eq(MotivoMovimentacao.CANCELAMENTO),
                    anyString()
            );
        }

        @Test
        @DisplayName("não deve estornar pagamento que já está em preparo")
        void naoDeveEstornarPedidoEmPreparo() {
            Pagamento pagamento = criarPagamentoPendente(pedidoReserva);
            pagamento.setStatus(StatusPagamento.APROVADO);
            pedidoReserva.setStatus(StatusPedido.EM_PREPARO);

            when(pagamentoRepository.findById(pagamento.getId()))
                    .thenReturn(Optional.of(pagamento));
            when(pedidoRepository.buscarCompletoPorId(pedidoReserva.getId()))
                    .thenReturn(Optional.of(pedidoReserva));

            assertThatThrownBy(() ->
                    pagamentoService.estornar(pagamento.getId())
            )
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("após o início do preparo");

            verify(movimentacaoEstoqueService, never())
                    .registrarEntrada(any(), anyInt(), any(), anyString());
        }
    }

    private Pedido criarPedidoComItem(TipoPedido tipoPedido) {
        Pedido pedido = new Pedido();
        pedido.setId(tipoPedido == TipoPedido.RESERVA ? 200L : 201L);
        pedido.setTipoPedido(tipoPedido);
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);

        ItemPedido item = new ItemPedido();
        item.setId(1L);
        item.setProduto(produto);
        item.setQuantidade(2);
        item.setPrecoUnitario(produto.getPrecoVenda());
        item.setPedido(pedido);

        pedido.setItens(new java.util.ArrayList<>(List.of(item)));
        pedido.setValorTotal(new BigDecimal("16.00"));

        return pedido;
    }

    private Pagamento criarPagamentoPendente(Pedido pedido) {
        Pagamento pagamento = new Pagamento();
        pagamento.setId(500L);
        pagamento.setPedido(pedido);
        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamento.setValor(pedido.getValorTotal());
        pagamento.setFormaPagamento(FormaPagamento.PIX);
        return pagamento;
    }
}