package com.senai.cantina.cantina.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.senai.cantina.cantina.exception.RecursoNaoEncontradoException;
import com.senai.cantina.cantina.model.Produto;
import com.senai.cantina.cantina.model.Produto.CategoriaProduto;
import com.senai.cantina.cantina.repository.ProdutoRepository;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(
            ProdutoRepository produtoRepository
    ) {
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public Produto cadastrar(Produto produto) {
        produto.setId(null);
        produto.setNome(produto.getNome().trim());
        produto.setEstoqueAtual(0);
        produto.setAtivo(true);

        return produtoRepository.save(produto);
    }

    @Transactional(readOnly = true)
    public List<Produto> listarTodos() {
        return produtoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Produto> listarAtivos() {
        return produtoRepository.findByAtivoTrue();
    }

    @Transactional(readOnly = true)
    public Produto buscarPorId(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(()
                        -> new RecursoNaoEncontradoException(
                        "produto",
                        id
                )
                );
    }

    @Transactional(readOnly = true)
    public List<Produto> filtrar(
            CategoriaProduto categoria,
            String nome
    ) {
        boolean possuiCategoria = categoria != null;
        boolean possuiNome
                = nome != null && !nome.isBlank();

        if (possuiCategoria && possuiNome) {
            return produtoRepository
                    .findByCategoriaAndNomeContainingIgnoreCaseAndAtivoTrue(
                            categoria,
                            nome.trim()
                    );
        }

        if (possuiCategoria) {
            return produtoRepository
                    .findByCategoriaAndAtivoTrue(
                            categoria
                    );
        }

        if (possuiNome) {
            return produtoRepository
                    .buscarProdutosAtivosPorNome(
                            nome.trim()
                    );
        }

        return produtoRepository.findByAtivoTrue();
    }

    @Transactional(readOnly = true)
    public List<Produto> listarComEstoqueBaixo() {
        return produtoRepository
                .buscarProdutosComEstoqueBaixo();
    }

    @Transactional
    public Produto atualizar(
            Long id,
            Produto dadosNovos
    ) {
        Produto existente = buscarPorId(id);

        existente.setNome(
                dadosNovos.getNome().trim()
        );

        existente.setDescricao(
                dadosNovos.getDescricao()
        );

        existente.setCategoria(
                dadosNovos.getCategoria()
        );

        existente.setPrecoCusto(
                dadosNovos.getPrecoCusto()
        );

        existente.setPrecoVenda(
                dadosNovos.getPrecoVenda()
        );

        existente.setEstoqueMinimo(
                dadosNovos.getEstoqueMinimo()
        );

        existente.setPerecivel(
                dadosNovos.isPerecivel()
        );

        return produtoRepository.save(existente);
    }

    @Transactional
    public void alterarStatus(
            Long id,
            boolean ativo
    ) {
        Produto produto = buscarPorId(id);
        produto.setAtivo(ativo);

        produtoRepository.save(produto);
    }
}
