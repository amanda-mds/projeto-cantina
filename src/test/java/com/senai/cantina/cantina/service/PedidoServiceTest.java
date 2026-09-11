package com.senai.cantina.cantina.service;

import com.senai.cantina.cantina.exception.RecursoNaoEncontradoException;
import com.senai.cantina.cantina.exception.RegraNegocioException;
import com.senai.cantina.cantina.model.Pedido;
import com.senai.cantina.cantina.model.Pedido.StatusPedido;
import com.senai.cantina.cantina.model.Pedido.TipoPedido;
import com.senai.cantina.cantina.model.Produto;
import com.senai.cantina.cantina.model.Usuario;
import com.senai.cantina.cantina.repository.ItemPedidoRepository;
import com.senai.cantina.cantina.repository.PedidoRepository;
import com.senai.cantina.cantina.repository.ProdutoRepository;
import com.senai.cantina.cantina.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoService")
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ItemPedidoRepository itemPedidoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PedidoService pedidoService;

    private Usuario usuarioAtivo;
    private Produto produtoDisponivel;

    @BeforeEach
    void configurarCenarioBase() {
        usuarioAtivo = new Usuario();
        usuarioAtivo.setId(1L);
        usuarioAtivo.setNome("Amanda");
        usuarioAtivo.setEmail("amanda@teste.com");
        usuarioAtivo.setAtivo(true);

        produtoDisponivel = new Produto();
        produtoDisponivel.setId(10L);
        produtoDisponivel.setNome("Coxinha");
        produtoDisponivel.setPrecoVenda(new BigDecimal("8.00"));
        produtoDisponivel.setEstoqueAtual(5);
        produtoDisponivel.setAtivo(true);
    }

    @Nested
    @DisplayName("criar()")
    class Criar {

        @Test
        @DisplayName("deve criar pedido do tipo PRESENCIAL com sucesso")
        void deveCriarPedidoPresencialComSucesso() {
            when(usuarioRepository.findById(1L))
                    .thenReturn(Optional.of(usuarioAtivo));
            when(pedidoRepository.save(any(Pedido.class)))
                    .thenAnswer(chamada -> chamada.getArgument(0));

            Pedido dadosPedido = new Pedido();
            dadosPedido.setTipoPedido(TipoPedido.PRESENCIAL);

            Pedido resultado = pedidoService.criar(1L, dadosPedido);

            assertThat(resultado.getStatus())
                    .isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
            assertThat(resultado.getUsuario()).isEqualTo(usuarioAtivo);
            assertThat(resultado.getValorTotal())
                    .isEqualByComparingTo(BigDecimal.ZERO);

            verify(pedidoRepository).save(any(Pedido.class));
        }

        @Test
        @DisplayName("não deve permitir pedido para usuário inativo")
        void naoDevePermitirPedidoParaUsuarioInativo() {
            usuarioAtivo.setAtivo(false);

            when(usuarioRepository.findById(1L))
                    .thenReturn(Optional.of(usuarioAtivo));

            Pedido dadosPedido = new Pedido();
            dadosPedido.setTipoPedido(TipoPedido.PRESENCIAL);

            assertThatThrownBy(() -> pedidoService.criar(1L, dadosPedido))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("usuário inativo");

            verify(pedidoRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve exigir horário de retirada futuro para pedidos do tipo RESERVA")
        void deveExigirHorarioDeRetiradaFuturoParaReserva() {
            when(usuarioRepository.findById(1L))
                    .thenReturn(Optional.of(usuarioAtivo));

            Pedido dadosPedido = new Pedido();
            dadosPedido.setTipoPedido(TipoPedido.RESERVA);
            dadosPedido.setHorarioRetirada(
                    LocalDateTime.now().minusHours(1) // horário no passado
            );

            assertThatThrownBy(() -> pedidoService.criar(1L, dadosPedido))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("futuro");
        }

        @Test
        @DisplayName("deve lançar exceção quando o usuário não existe")
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            when(usuarioRepository.findById(99L))
                    .thenReturn(Optional.empty());

            Pedido dadosPedido = new Pedido();

            assertThatThrownBy(() -> pedidoService.criar(99L, dadosPedido))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }
    }

    @Nested
    @DisplayName("adicionarItem()")
    class AdicionarItem {

        @Test
        @DisplayName("deve adicionar item e recalcular o valor total do pedido")
        void deveAdicionarItemERecalcularTotal() {
            Pedido pedido = criarPedidoEditavel();

            when(pedidoRepository.buscarCompletoPorId(pedido.getId()))
                    .thenReturn(Optional.of(pedido));
            when(produtoRepository.findById(10L))
                    .thenReturn(Optional.of(produtoDisponivel));
            when(pedidoRepository.save(any(Pedido.class)))
                    .thenAnswer(chamada -> chamada.getArgument(0));

            Pedido resultado = pedidoService.adicionarItem(
                    pedido.getId(), 10L, 2
            );

            // 2 unidades x R$ 8,00 = R$ 16,00
            assertThat(resultado.getValorTotal())
                    .isEqualByComparingTo(new BigDecimal("16.00"));
            assertThat(resultado.getItens()).hasSize(1);
        }

        @Test
        @DisplayName("não deve adicionar item além do estoque disponível")
        void naoDeveAdicionarItemAlemDoEstoqueDisponivel() {
            Pedido pedido = criarPedidoEditavel();
            produtoDisponivel.setEstoqueAtual(3);

            when(pedidoRepository.buscarCompletoPorId(pedido.getId()))
                    .thenReturn(Optional.of(pedido));
            when(produtoRepository.findById(10L))
                    .thenReturn(Optional.of(produtoDisponivel));

            assertThatThrownBy(() ->
                    pedidoService.adicionarItem(pedido.getId(), 10L, 5)
            )
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Estoque insuficiente");

            verify(pedidoRepository, never()).save(any());
        }

        @Test
        @DisplayName("não deve adicionar produto inativo ao pedido")
        void naoDeveAdicionarProdutoInativo() {
            Pedido pedido = criarPedidoEditavel();
            produtoDisponivel.setAtivo(false);

            when(pedidoRepository.buscarCompletoPorId(pedido.getId()))
                    .thenReturn(Optional.of(pedido));
            when(produtoRepository.findById(10L))
                    .thenReturn(Optional.of(produtoDisponivel));

            assertThatThrownBy(() ->
                    pedidoService.adicionarItem(pedido.getId(), 10L, 1)
            )
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("não está disponível");
        }

        @Test
        @DisplayName("não deve permitir adicionar item em pedido que não está aguardando pagamento")
        void naoDevePermitirAdicionarItemEmPedidoJaPago() {
            Pedido pedido = criarPedidoEditavel();
            pedido.setStatus(StatusPedido.PAGO);

            when(pedidoRepository.buscarCompletoPorId(pedido.getId()))
                    .thenReturn(Optional.of(pedido));

            assertThatThrownBy(() ->
                    pedidoService.adicionarItem(pedido.getId(), 10L, 1)
            )
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("não pode mais ser alterado");

            verify(produtoRepository, never()).findById(any());
        }

        @Test
        @DisplayName("não deve aceitar quantidade zero ou negativa")
        void naoDeveAceitarQuantidadeInvalida() {
            assertThatThrownBy(() ->
                    pedidoService.adicionarItem(1L, 10L, 0)
            )
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("maior que zero");

            verifyNoInteractions(pedidoRepository);
        }
    }

    @Nested
    @DisplayName("Transições de status do pedido")
    class TransicoesDeStatus {

        @Test
        @DisplayName("deve iniciar o preparo apenas quando o pedido estiver PAGO")
        void deveIniciarPreparoApenasQuandoPago() {
            Pedido pedido = criarPedidoEditavel();
            pedido.setStatus(StatusPedido.PAGO);

            when(pedidoRepository.buscarCompletoPorId(pedido.getId()))
                    .thenReturn(Optional.of(pedido));
            when(pedidoRepository.save(any(Pedido.class)))
                    .thenAnswer(chamada -> chamada.getArgument(0));

            Pedido resultado = pedidoService.iniciarPreparo(pedido.getId());

            assertThat(resultado.getStatus())
                    .isEqualTo(StatusPedido.EM_PREPARO);
        }

        @Test
        @DisplayName("não deve iniciar o preparo se o pedido ainda não foi pago")
        void naoDeveIniciarPreparoSeNaoPago() {
            Pedido pedido = criarPedidoEditavel(); // status AGUARDANDO_PAGAMENTO

            when(pedidoRepository.buscarCompletoPorId(pedido.getId()))
                    .thenReturn(Optional.of(pedido));

            assertThatThrownBy(() ->
                    pedidoService.iniciarPreparo(pedido.getId())
            )
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Não é possível iniciar o preparo");

            verify(pedidoRepository, never()).save(any());
        }

        @Test
        @DisplayName("não deve marcar como pronto um pedido que ainda não está em preparo")
        void naoDeveMarcarComoProntoSemEstarEmPreparo() {
            Pedido pedido = criarPedidoEditavel();
            pedido.setStatus(StatusPedido.PAGO);

            when(pedidoRepository.buscarCompletoPorId(pedido.getId()))
                    .thenReturn(Optional.of(pedido));

            assertThatThrownBy(() ->
                    pedidoService.marcarComoPronto(pedido.getId())
            )
                    .isInstanceOf(RegraNegocioException.class);
        }

        @Test
        @DisplayName("deve cancelar pedido pendente de pagamento")
        void deveCancelarPedidoPendente() {
            Pedido pedido = criarPedidoEditavel(); // AGUARDANDO_PAGAMENTO

            when(pedidoRepository.buscarCompletoPorId(pedido.getId()))
                    .thenReturn(Optional.of(pedido));
            when(pedidoRepository.save(any(Pedido.class)))
                    .thenAnswer(chamada -> chamada.getArgument(0));

            Pedido resultado = pedidoService.cancelarPendente(pedido.getId());

            assertThat(resultado.getStatus())
                    .isEqualTo(StatusPedido.CANCELADO);
        }

        @Test
        @DisplayName("não deve cancelar um pedido que já está em preparo")
        void naoDeveCancelarPedidoEmPreparo() {
            Pedido pedido = criarPedidoEditavel();
            pedido.setStatus(StatusPedido.EM_PREPARO);

            when(pedidoRepository.buscarCompletoPorId(pedido.getId()))
                    .thenReturn(Optional.of(pedido));

            assertThatThrownBy(() ->
                    pedidoService.cancelarPendente(pedido.getId())
            )
                    .isInstanceOf(RegraNegocioException.class);
        }
    }

    @Nested
    @DisplayName("confirmarPedido()")
    class ConfirmarPedido {

        @Test
        @DisplayName("não deve confirmar pedido sem nenhum item")
        void naoDeveConfirmarPedidoSemItens() {
            Pedido pedido = criarPedidoEditavel(); // sem itens

            when(pedidoRepository.buscarCompletoPorId(pedido.getId()))
                    .thenReturn(Optional.of(pedido));

            assertThatThrownBy(() ->
                    pedidoService.confirmarPedido(pedido.getId())
            )
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("pelo menos um item");
        }
    }

    private Pedido criarPedidoEditavel() {
        Pedido pedido = new Pedido();
        pedido.setId(100L);
        pedido.setUsuario(usuarioAtivo);
        pedido.setTipoPedido(TipoPedido.PRESENCIAL);
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        pedido.setValorTotal(BigDecimal.ZERO);
        return pedido;
    }
}
