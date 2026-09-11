package com.senai.cantina.cantina.repository;

import java.util.List;

import com.senai.cantina.cantina.model.Produto;
import com.senai.cantina.cantina.model.Produto.CategoriaProduto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProdutoRepository
        extends JpaRepository<Produto, Long> {

    List<Produto> findByAtivoTrue();

    List<Produto> findByCategoriaAndAtivoTrue(
            CategoriaProduto categoria
    );

    List<Produto> findByNomeContainingIgnoreCase(
            String nome
    );

    @Query("""
            SELECT p
            FROM Produto p
            WHERE p.estoqueAtual <= p.estoqueMinimo
            AND p.ativo = true
            """)
    List<Produto> buscarProdutosComEstoqueBaixo();

    @Query("""
            SELECT p
            FROM Produto p
            WHERE LOWER(p.nome)
            LIKE LOWER(CONCAT('%', :nome, '%'))
            AND p.ativo = true
            """)
    List<Produto> buscarProdutosAtivosPorNome(
            @Param("nome") String nome
    );

    List<Produto> findByCategoriaAndNomeContainingIgnoreCaseAndAtivoTrue(
            CategoriaProduto categoria,
            String nome
    );
}