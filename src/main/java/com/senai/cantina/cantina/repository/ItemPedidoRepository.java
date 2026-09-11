package com.senai.cantina.cantina.repository;

import java.util.List;

import com.senai.cantina.cantina.model.ItemPedido;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemPedidoRepository
        extends JpaRepository<ItemPedido, Long> {

    List<ItemPedido> findByPedidoId(
            Long pedidoId
    );

    List<ItemPedido> findByProdutoId(
            Long produtoId
    );
}