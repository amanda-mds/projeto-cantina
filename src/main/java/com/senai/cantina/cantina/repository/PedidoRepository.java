package com.senai.cantina.cantina.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.senai.cantina.cantina.model.Pedido;
import com.senai.cantina.cantina.model.Pedido.StatusPedido;
import com.senai.cantina.cantina.model.Pedido.TipoPedido;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository
        extends JpaRepository<Pedido, Long> {

    @Override
    @EntityGraph(attributePaths = {"usuario"})
    List<Pedido> findAll();

    @EntityGraph(attributePaths = {"usuario"})
    List<Pedido> findByUsuarioIdOrderByDataHoraPedidoDesc(
            Long usuarioId
    );

    @EntityGraph(attributePaths = {"usuario"})
    List<Pedido> findByStatus(
            StatusPedido status
    );

    @EntityGraph(attributePaths = {"usuario"})
    List<Pedido> findByTipoPedido(
            TipoPedido tipoPedido
    );

    @EntityGraph(attributePaths = {"usuario"})
    List<Pedido> findByStatusAndTipoPedido(
            StatusPedido status,
            TipoPedido tipoPedido
    );

    @EntityGraph(attributePaths = {"usuario"})
    List<Pedido> findByDataHoraPedidoBetween(
            LocalDateTime inicio,
            LocalDateTime fim
    );

    Optional<Pedido> findByCodigoRetirada(
            String codigoRetirada
    );

    @EntityGraph(
            attributePaths = {
                    "usuario",
                    "itens",
                    "itens.produto"
            }
    )
    @Query("""
        SELECT p
        FROM Pedido p
        WHERE p.id = :id
        """)
    Optional<Pedido> buscarCompletoPorId(
            @Param("id") Long id
    );
}