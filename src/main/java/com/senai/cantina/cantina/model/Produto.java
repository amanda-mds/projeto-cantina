package com.senai.cantina.cantina.model;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome do produto é obrigatório")
    @Size(min = 2, max = 100,
            message = "Nome deve conter entre 2 e 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nome;

    @Size(max = 500,
            message = "Descrição deve conter no máximo 500 caracteres")
    @Column(columnDefinition = "TEXT")
    private String descricao;

    @NotNull(message = "Categoria é obrigatória")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaProduto categoria;

    @NotNull(message = "Preço de custo é obrigatório")
    @DecimalMin(value = "0.00",
            message = "Preço de custo não pode ser negativo")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoCusto = BigDecimal.ZERO;

    @NotNull(message = "Preço de venda é obrigatório")
    @DecimalMin(value = "0.01",
            message = "Preço de venda deve ser maior que zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoVenda;

    @PositiveOrZero(message = "Estoque atual não pode ser negativo")
    @Column(nullable = false)
    private int estoqueAtual;

    @PositiveOrZero(message = "Estoque mínimo não pode ser negativo")
    @Column(nullable = false)
    private int estoqueMinimo;

    @Column(nullable = false)
    private boolean perecivel;

    @Column(nullable = false)
    private boolean ativo = true;

    public Produto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public CategoriaProduto getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaProduto categoria) {
        this.categoria = categoria;
    }

    public BigDecimal getPrecoCusto() {
        return precoCusto;
    }

    public void setPrecoCusto(BigDecimal precoCusto) {
        this.precoCusto = precoCusto;
    }

    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    public void setPrecoVenda(BigDecimal precoVenda) {
        this.precoVenda = precoVenda;
    }

    public int getEstoqueAtual() {
        return estoqueAtual;
    }

    public void setEstoqueAtual(int estoqueAtual) {
        this.estoqueAtual = estoqueAtual;
    }

    public int getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public void setEstoqueMinimo(int estoqueMinimo) {
        this.estoqueMinimo = estoqueMinimo;
    }

    public boolean isPerecivel() {
        return perecivel;
    }

    public void setPerecivel(boolean perecivel) {
        this.perecivel = perecivel;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Produto produto = (Produto) o;
        return id != null && id.equals(produto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }

    public enum CategoriaProduto {

        LANCHE("Lanche"),
        SALGADO("Salgado"),
        SNACK("Snack e porção"),
        BEBIDA("Bebida"),
        SOBREMESA("Sobremesa"),
        ADICIONAL("Adicional"),
        OUTROS("Outros");

        private final String descricao;

        CategoriaProduto(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }
}