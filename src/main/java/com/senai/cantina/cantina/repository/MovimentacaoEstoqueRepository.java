package com.senai.cantina.cantina.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.senai.cantina.cantina.model.MovimentacaoEstoque;
import com.senai.cantina.cantina.model.MovimentacaoEstoque.MotivoMovimentacao;
import com.senai.cantina.cantina.model.MovimentacaoEstoque.TipoMovimentacao;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoEstoqueRepository
        extends JpaRepository<MovimentacaoEstoque, Long> {

    List<MovimentacaoEstoque>
    findByProdutoIdOrderByDataHoraDesc(
            Long produtoId
    );

    List<MovimentacaoEstoque> findByTipo(
            TipoMovimentacao tipo
    );

    List<MovimentacaoEstoque> findByMotivo(
            MotivoMovimentacao motivo
    );

    List<MovimentacaoEstoque> findByDataHoraBetween(
            LocalDateTime inicio,
            LocalDateTime fim
    );
}