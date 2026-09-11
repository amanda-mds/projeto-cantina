package com.senai.cantina.cantina.exception;

public class RecursoNaoEncontradoException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RecursoNaoEncontradoException(
            String mensagem
    ) {
        super(mensagem);
    }

    public RecursoNaoEncontradoException(
            String recurso,
            Long id
    ) {
        super(
                "Não foi possível encontrar "
                        + recurso
                        + " com o ID "
                        + id
                        + "."
        );
    }
}