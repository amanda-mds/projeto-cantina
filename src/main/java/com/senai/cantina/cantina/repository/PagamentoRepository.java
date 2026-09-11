package com.senai.cantina.cantina.repository;

import java.util.List;
import java.util.Optional;

import com.senai.cantina.cantina.model.Pagamento;
import com.senai.cantina.cantina.model.Pagamento.StatusPagamento;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PagamentoRepository
        extends JpaRepository<Pagamento, Long> {

    Optional<Pagamento> findByPedidoId(
            Long pedidoId
    );

    Optional<Pagamento> findByCodigoTransacao(
            String codigoTransacao
    );

    List<Pagamento> findByStatus(
            StatusPagamento status
    );
}