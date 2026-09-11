package com.senai.cantina.cantina.model;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "movimentacoes_estoque")
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Produto é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @NotNull(message = "Tipo de movimentação é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimentacao tipo;

    @NotNull(message = "Motivo da movimentação é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MotivoMovimentacao motivo;

    @Positive(message = "Quantidade deve ser maior que zero")
    @Column(nullable = false)
    private int quantidade;

    @PositiveOrZero(
            message = "Saldo anterior não pode ser negativo"
    )
    @Column(nullable = false)
    private int saldoAnterior;

    @PositiveOrZero(
            message = "Saldo atual não pode ser negativo"
    )
    @Column(nullable = false)
    private int saldoAtual;

    @NotNull(message = "Data e hora são obrigatórias")
    @Column(nullable = false)
    private LocalDateTime dataHora =
            LocalDateTime.now();

    @Size(
            max = 500,
            message = "Observação deve conter no máximo 500 caracteres"
    )
    @Column(columnDefinition = "TEXT")
    private String observacao;
    public MovimentacaoEstoque() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public TipoMovimentacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimentacao tipo) {
        this.tipo = tipo;
    }

    public MotivoMovimentacao getMotivo() {
        return motivo;
    }

    public void setMotivo(
            MotivoMovimentacao motivo
    ) {
        this.motivo = motivo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public int getSaldoAnterior() {
        return saldoAnterior;
    }

    public void setSaldoAnterior(int saldoAnterior) {
        this.saldoAnterior = saldoAnterior;
    }

    public int getSaldoAtual() {
        return saldoAtual;
    }

    public void setSaldoAtual(int saldoAtual) {
        this.saldoAtual = saldoAtual;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MovimentacaoEstoque movimentacao =
                (MovimentacaoEstoque) o;

        return id != null
                && id.equals(movimentacao.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }

    public enum TipoMovimentacao {

        ENTRADA("Entrada"),
        SAIDA("Saída");

        private final String descricao;

        TipoMovimentacao(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public enum MotivoMovimentacao {

        COMPRA("Compra"),
        VENDA("Venda"),
        PERDA("Perda ou desperdício"),
        AJUSTE("Ajuste manual"),
        CANCELAMENTO("Cancelamento de pedido");

        private final String descricao;

        MotivoMovimentacao(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }
}