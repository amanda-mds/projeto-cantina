package com.senai.cantina.cantina.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "pagamentos")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Pedido é obrigatório")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "pedido_id",
            nullable = false,
            unique = true
    )
    private Pedido pedido;

    @NotNull(message = "Forma de pagamento é obrigatória")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormaPagamento formaPagamento;

    @NotNull(message = "Status do pagamento é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPagamento status =
            StatusPagamento.PENDENTE;

    @NotNull(message = "Valor do pagamento é obrigatório")
    @DecimalMin(value = "0.01",
            message = "Valor do pagamento deve ser maior que zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column
    private LocalDateTime dataHoraPagamento;

    @Column(unique = true, length = 150)
    private String codigoTransacao;

    public Pagamento() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(
            FormaPagamento formaPagamento
    ) {
        this.formaPagamento = formaPagamento;
    }

    public StatusPagamento getStatus() {
        return status;
    }

    public void setStatus(StatusPagamento status) {
        this.status = status;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDateTime getDataHoraPagamento() {
        return dataHoraPagamento;
    }

    public void setDataHoraPagamento(
            LocalDateTime dataHoraPagamento
    ) {
        this.dataHoraPagamento = dataHoraPagamento;
    }

    public String getCodigoTransacao() {
        return codigoTransacao;
    }

    public void setCodigoTransacao(String codigoTransacao) {
        this.codigoTransacao = codigoTransacao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Pagamento pagamento = (Pagamento) o;
        return id != null && id.equals(pagamento.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }

    public enum FormaPagamento {

        PIX("Pix"),
        CREDITO("Cartão de crédito"),
        DEBITO("Cartão de débito"),
        DINHEIRO("Dinheiro");

        private final String descricao;

        FormaPagamento(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public enum StatusPagamento {

        PENDENTE("Pendente"),
        APROVADO("Aprovado"),
        RECUSADO("Recusado"),
        CANCELADO("Cancelado"),
        ESTORNADO("Estornado");

        private final String descricao;

        StatusPagamento(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }
}