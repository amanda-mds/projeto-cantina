package com.senai.cantina.cantina.service;

import com.senai.cantina.cantina.exception.RegraNegocioException;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.senai.cantina.cantina.exception.RecursoNaoEncontradoException;
import com.senai.cantina.cantina.model.MovimentacaoEstoque;
import com.senai.cantina.cantina.model.MovimentacaoEstoque.MotivoMovimentacao;
import com.senai.cantina.cantina.model.MovimentacaoEstoque.TipoMovimentacao;
import com.senai.cantina.cantina.model.Produto;
import com.senai.cantina.cantina.repository.MovimentacaoEstoqueRepository;
import com.senai.cantina.cantina.repository.ProdutoRepository;

@Service
public class MovimentacaoEstoqueService {

    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final ProdutoRepository produtoRepository;

    public MovimentacaoEstoqueService(
            MovimentacaoEstoqueRepository movimentacaoRepository,
            ProdutoRepository produtoRepository
    ) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public MovimentacaoEstoque registrarEntrada(
            Long produtoId,
            int quantidade,
            MotivoMovimentacao motivo,
            String observacao
    ) {
        validarQuantidade(quantidade);

        if (motivo != MotivoMovimentacao.COMPRA
                && motivo != MotivoMovimentacao.CANCELAMENTO
                && motivo != MotivoMovimentacao.AJUSTE) {

            throw new RegraNegocioException(
                    "Motivo inválido para uma entrada de estoque."
            );
        }

        Produto produto = buscarProduto(produtoId);

        if (!produto.isAtivo()
                && motivo != MotivoMovimentacao.CANCELAMENTO) {

            throw new RegraNegocioException(
                    "Não é possível registrar entrada para o produto "
                    + produto.getNome()
                    + " pois ele está inativo."
            );
        }

        int saldoAnterior = produto.getEstoqueAtual();
        int saldoAtual = saldoAnterior + quantidade;

        produto.setEstoqueAtual(saldoAtual);
        produtoRepository.save(produto);

        return salvarMovimentacao(
                produto,
                TipoMovimentacao.ENTRADA,
                motivo,
                quantidade,
                saldoAnterior,
                saldoAtual,
                observacao
        );
    }

    @Transactional
    public MovimentacaoEstoque registrarSaida(
            Long produtoId,
            int quantidade,
            MotivoMovimentacao motivo,
            String observacao
    ) {
        validarQuantidade(quantidade);

        if (motivo != MotivoMovimentacao.VENDA
                && motivo != MotivoMovimentacao.PERDA
                && motivo != MotivoMovimentacao.AJUSTE) {

            throw new RegraNegocioException(
                    "Motivo inválido para uma saída de estoque."
            );
        }

        Produto produto = buscarProduto(produtoId);

        if (!produto.isAtivo()
                && motivo == MotivoMovimentacao.PERDA) {

            throw new RegraNegocioException(
                    "Não é possível registrar perda para o produto "
                    + produto.getNome()
                    + " pois ele está inativo."
            );
        }

        int saldoAnterior = produto.getEstoqueAtual();

        if (saldoAnterior < quantidade) {
            throw new RegraNegocioException(
                    "Estoque insuficiente para o produto "
                    + produto.getNome()
                    + ". Disponível: "
                    + saldoAnterior
                    + "."
            );
        }

        int saldoAtual = saldoAnterior - quantidade;

        produto.setEstoqueAtual(saldoAtual);
        produtoRepository.save(produto);

        return salvarMovimentacao(
                produto,
                TipoMovimentacao.SAIDA,
                motivo,
                quantidade,
                saldoAnterior,
                saldoAtual,
                observacao
        );
    }

    @Transactional
    public MovimentacaoEstoque registrarPerda(
            Long produtoId,
            int quantidade,
            String observacao
    ) {
        return registrarSaida(
                produtoId,
                quantidade,
                MotivoMovimentacao.PERDA,
                observacao
        );
    }

    @Transactional
    public MovimentacaoEstoque ajustarEstoque(
            Long produtoId,
            int novoSaldo,
            String observacao
    ) {
        if (novoSaldo < 0) {
            throw new RegraNegocioException(
                    "O novo saldo não pode ser negativo."
            );
        }

        Produto produto = buscarProduto(produtoId);
        int saldoAnterior = produto.getEstoqueAtual();

        if (saldoAnterior == novoSaldo) {
            throw new RegraNegocioException(
                    "O novo saldo é igual ao saldo atual."
            );
        }

        int quantidade
                = Math.abs(novoSaldo - saldoAnterior);

        TipoMovimentacao tipo;

        if (novoSaldo > saldoAnterior) {
            tipo = TipoMovimentacao.ENTRADA;
        } else {
            tipo = TipoMovimentacao.SAIDA;
        }

        produto.setEstoqueAtual(novoSaldo);
        produtoRepository.save(produto);

        return salvarMovimentacao(
                produto,
                tipo,
                MotivoMovimentacao.AJUSTE,
                quantidade,
                saldoAnterior,
                novoSaldo,
                observacao
        );
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoque> listarTodas() {
        return movimentacaoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoque> listarPorProduto(
            Long produtoId
    ) {
        buscarProduto(produtoId);

        return movimentacaoRepository
                .findByProdutoIdOrderByDataHoraDesc(
                        produtoId
                );
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoque> listarPorMotivo(
            MotivoMovimentacao motivo
    ) {
        if (motivo == null) {
            return movimentacaoRepository.findAll();
        }

        return movimentacaoRepository
                .findByMotivo(motivo);
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoque> listarPorPeriodo(
            LocalDateTime inicio,
            LocalDateTime fim
    ) {
        if (inicio == null || fim == null) {
            throw new RegraNegocioException(
                    "Informe o início e o fim do período."
            );
        }

        if (inicio.isAfter(fim)) {
            throw new RegraNegocioException(
                    "A data inicial não pode ser posterior à data final."
            );
        }

        return movimentacaoRepository
                .findByDataHoraBetween(inicio, fim);
    }

    private MovimentacaoEstoque salvarMovimentacao(
            Produto produto,
            TipoMovimentacao tipo,
            MotivoMovimentacao motivo,
            int quantidade,
            int saldoAnterior,
            int saldoAtual,
            String observacao
    ) {
        MovimentacaoEstoque movimentacao
                = new MovimentacaoEstoque();

        movimentacao.setProduto(produto);
        movimentacao.setTipo(tipo);
        movimentacao.setMotivo(motivo);
        movimentacao.setQuantidade(quantidade);
        movimentacao.setSaldoAnterior(saldoAnterior);
        movimentacao.setSaldoAtual(saldoAtual);
        movimentacao.setDataHora(LocalDateTime.now());
        movimentacao.setObservacao(observacao);

        return movimentacaoRepository.save(
                movimentacao
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

    private void validarQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new RegraNegocioException(
                    "A quantidade deve ser maior que zero."
            );
        }
    }
}
