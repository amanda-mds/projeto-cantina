package com.senai.cantina.cantina.service;

import com.senai.cantina.cantina.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.senai.cantina.cantina.exception.RecursoNaoEncontradoException;
import com.senai.cantina.cantina.model.ItemPedido;
import com.senai.cantina.cantina.model.Pedido;
import com.senai.cantina.cantina.model.Pedido.StatusPedido;
import com.senai.cantina.cantina.model.Pedido.TipoPedido;
import com.senai.cantina.cantina.model.Produto;
import com.senai.cantina.cantina.model.Usuario;
import com.senai.cantina.cantina.repository.ItemPedidoRepository;
import com.senai.cantina.cantina.repository.PedidoRepository;
import com.senai.cantina.cantina.repository.ProdutoRepository;
import com.senai.cantina.cantina.repository.UsuarioRepository;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemPedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ItemPedidoRepository itemPedidoRepository,
            ProdutoRepository produtoRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.pedidoRepository = pedidoRepository;
        this.itemPedidoRepository = itemPedidoRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Pedido criar(
            Long usuarioId,
            Pedido dadosPedido
    ) {
        Usuario usuario = buscarUsuario(usuarioId);

        if (!usuario.isAtivo()) {
            throw new RegraNegocioException(
                    "Não é possível criar um pedido para um usuário inativo."
            );
        }

        TipoPedido tipoPedido
                = dadosPedido.getTipoPedido();

        if (tipoPedido == null) {
            tipoPedido = TipoPedido.RESERVA;
        }

        LocalDateTime horarioRetirada
                = dadosPedido.getHorarioRetirada();

        if (tipoPedido == TipoPedido.RESERVA) {
            validarHorarioRetirada(horarioRetirada);
        } else {
            horarioRetirada = null;
        }

        Pedido pedido = new Pedido();

        pedido.setUsuario(usuario);
        pedido.setDataHoraPedido(
                LocalDateTime.now()
        );
        pedido.setHorarioRetirada(
                horarioRetirada
        );
        pedido.setTipoPedido(tipoPedido);
        pedido.setValorTotal(BigDecimal.ZERO);
        pedido.setStatus(
                StatusPedido.AGUARDANDO_PAGAMENTO
        );
        pedido.setObservacao(
                dadosPedido.getObservacao()
        );
        pedido.setCodigoRetirada(null);

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido adicionarItem(
            Long pedidoId,
            Long produtoId,
            int quantidade
    ) {
        validarQuantidade(quantidade);

        Pedido pedido = buscarPorId(pedidoId);
        validarPedidoEditavel(pedido);

        Produto produto = buscarProduto(produtoId);

        if (!produto.isAtivo()) {
            throw new RegraNegocioException(
                    "O produto "
                    + produto.getNome()
                    + " não está disponível."
            );
        }

        ItemPedido itemExistente
                = buscarItemDoProduto(
                        pedido,
                        produtoId
                );

        int quantidadeTotal = quantidade;

        if (itemExistente != null) {
            quantidadeTotal
                    += itemExistente.getQuantidade();
        }

        if (produto.getEstoqueAtual()
                < quantidadeTotal) {

            throw new RegraNegocioException(
                    "Estoque insuficiente para o produto "
                    + produto.getNome()
                    + ". Disponível: "
                    + produto.getEstoqueAtual()
                    + "."
            );
        }

        if (itemExistente == null) {
            ItemPedido novoItem
                    = new ItemPedido();

            novoItem.setPedido(pedido);
            novoItem.setProduto(produto);
            novoItem.setQuantidade(quantidade);
            novoItem.setPrecoUnitario(
                    produto.getPrecoVenda()
            );

            pedido.adicionarItem(novoItem);
            itemPedidoRepository.save(novoItem);
        } else {
            itemExistente.setQuantidade(
                    quantidadeTotal
            );

            itemExistente.setPrecoUnitario(
                    produto.getPrecoVenda()
            );

            itemPedidoRepository.save(
                    itemExistente
            );
        }

        recalcularTotal(pedido);

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido alterarQuantidade(
            Long pedidoId,
            Long itemId,
            int quantidade
    ) {
        validarQuantidade(quantidade);

        Pedido pedido = buscarPorId(pedidoId);
        validarPedidoEditavel(pedido);

        ItemPedido item
                = buscarItemDoPedido(
                        pedido,
                        itemId
                );

        Produto produto = item.getProduto();

        if (produto.getEstoqueAtual()
                < quantidade) {

            throw new RegraNegocioException(
                    "Estoque insuficiente para o produto "
                    + produto.getNome()
                    + ". Disponível: "
                    + produto.getEstoqueAtual()
                    + "."
            );
        }

        item.setQuantidade(quantidade);
        item.setPrecoUnitario(
                produto.getPrecoVenda()
        );

        itemPedidoRepository.save(item);

        recalcularTotal(pedido);

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido removerItem(
            Long pedidoId,
            Long itemId
    ) {
        Pedido pedido = buscarPorId(pedidoId);
        validarPedidoEditavel(pedido);

        ItemPedido item
                = buscarItemDoPedido(
                        pedido,
                        itemId
                );

        pedido.removerItem(item);

        recalcularTotal(pedido);

        return pedidoRepository.save(pedido);
    }

    @Transactional(readOnly = true)
    public Pedido buscarPorId(Long id) {
        return pedidoRepository
                .buscarCompletoPorId(id)
                .orElseThrow(()
                        -> new RecursoNaoEncontradoException(
                        "pedido",
                        id
                )
                );
    }

    @Transactional(readOnly = true)
    public Pedido buscarPorCodigoRetirada(
            String codigoRetirada
    ) {
        if (codigoRetirada == null
                || codigoRetirada.isBlank()) {

            throw new RegraNegocioException(
                    "Informe o código de retirada."
            );
        }

        return pedidoRepository
                .findByCodigoRetirada(
                        codigoRetirada.trim()
                )
                .orElseThrow(()
                        -> new RecursoNaoEncontradoException(
                        "Pedido não encontrado para o código informado."
                )
                );
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarPorUsuario(
            Long usuarioId
    ) {
        buscarUsuario(usuarioId);

        return pedidoRepository
                .findByUsuarioIdOrderByDataHoraPedidoDesc(
                        usuarioId
                );
    }

    @Transactional(readOnly = true)
    public List<Pedido> filtrar(
            StatusPedido status,
            TipoPedido tipoPedido
    ) {
        if (status != null
                && tipoPedido != null) {

            return pedidoRepository
                    .findByStatusAndTipoPedido(
                            status,
                            tipoPedido
                    );
        }

        if (status != null) {
            return pedidoRepository
                    .findByStatus(status);
        }

        if (tipoPedido != null) {
            return pedidoRepository
                    .findByTipoPedido(tipoPedido);
        }

        return pedidoRepository.findAll();
    }

    @Transactional
    public Pedido confirmarPedido(Long id) {
        Pedido pedido = buscarPorId(id);
        validarPedidoEditavel(pedido);

        if (pedido.getItens().isEmpty()) {
            throw new RegraNegocioException(
                    "O pedido precisa ter pelo menos um item."
            );
        }

        if (pedido.getTipoPedido()
                == TipoPedido.RESERVA) {

            validarHorarioRetirada(
                    pedido.getHorarioRetirada()
            );
        }

        recalcularTotal(pedido);

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido iniciarPreparo(Long id) {
        Pedido pedido = buscarPorId(id);

        validarStatus(
                pedido,
                StatusPedido.PAGO,
                "iniciar o preparo"
        );

        pedido.setStatus(
                StatusPedido.EM_PREPARO
        );

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido marcarComoPronto(Long id) {
        Pedido pedido = buscarPorId(id);

        validarStatus(
                pedido,
                StatusPedido.EM_PREPARO,
                "marcar o pedido como pronto"
        );

        pedido.setStatus(
                StatusPedido.PRONTO
        );

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido marcarComoRetirado(Long id) {
        Pedido pedido = buscarPorId(id);

        validarStatus(
                pedido,
                StatusPedido.PRONTO,
                "marcar o pedido como retirado"
        );

        pedido.setStatus(
                StatusPedido.RETIRADO
        );

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido cancelarPendente(Long id) {
        Pedido pedido = buscarPorId(id);

        validarStatus(
                pedido,
                StatusPedido.AGUARDANDO_PAGAMENTO,
                "cancelar o pedido"
        );

        pedido.setStatus(
                StatusPedido.CANCELADO
        );

        return pedidoRepository.save(pedido);
    }

    private void recalcularTotal(Pedido pedido) {
        BigDecimal total = pedido.getItens()
                .stream()
                .map(ItemPedido::getSubtotal)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );

        pedido.setValorTotal(total);
    }

    private ItemPedido buscarItemDoProduto(
            Pedido pedido,
            Long produtoId
    ) {
        return pedido.getItens()
                .stream()
                .filter(item
                        -> item.getProduto()
                        .getId()
                        .equals(produtoId)
                )
                .findFirst()
                .orElse(null);
    }

    private ItemPedido buscarItemDoPedido(
            Pedido pedido,
            Long itemId
    ) {
        return pedido.getItens()
                .stream()
                .filter(item
                        -> item.getId().equals(itemId)
                )
                .findFirst()
                .orElseThrow(()
                        -> new RecursoNaoEncontradoException(
                        "item do pedido",
                        itemId
                )
                );
    }

    private Produto buscarProduto(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(()
                        -> new RecursoNaoEncontradoException(
                        "produto",
                        id
                )
                );
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(()
                        -> new RecursoNaoEncontradoException(
                        "usuário",
                        id
                )
                );
    }

    private void validarPedidoEditavel(
            Pedido pedido
    ) {
        if (pedido.getStatus()
                != StatusPedido.AGUARDANDO_PAGAMENTO) {

            throw new RegraNegocioException(
                    "O pedido não pode mais ser alterado. Status atual: "
                    + pedido.getStatus()
                            .getDescricao()
                    + "."
            );
        }
    }

    private void validarStatus(
            Pedido pedido,
            StatusPedido statusEsperado,
            String operacao
    ) {
        if (pedido.getStatus()
                != statusEsperado) {

            throw new RegraNegocioException(
                    "Não é possível "
                    + operacao
                    + ". Status atual: "
                    + pedido.getStatus()
                            .getDescricao()
                    + "."
            );
        }
    }

    private void validarHorarioRetirada(
            LocalDateTime horarioRetirada
    ) {
        if (horarioRetirada == null) {
            throw new RegraNegocioException(
                    "Informe o horário de retirada."
            );
        }

        if (!horarioRetirada.isAfter(
                LocalDateTime.now()
        )) {
            throw new RegraNegocioException(
                    "O horário de retirada deve ser futuro."
            );
        }
    }

    private void validarQuantidade(
            int quantidade
    ) {
        if (quantidade <= 0) {
            throw new RegraNegocioException(
                    "A quantidade deve ser maior que zero."
            );
        }
    }
}
