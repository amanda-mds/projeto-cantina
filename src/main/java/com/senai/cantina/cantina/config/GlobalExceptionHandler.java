package com.senai.cantina.cantina.config;

import com.senai.cantina.cantina.exception.RecursoNaoEncontradoException;
import com.senai.cantina.cantina.exception.RegraNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String tratarRecursoNaoEncontrado(
            RecursoNaoEncontradoException exception,
            Model model
    ) {
        model.addAttribute("mensagemErro", exception.getMessage());
        return "erro";
    }

    @ExceptionHandler(RegraNegocioException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String tratarRegraNegocio(
            RegraNegocioException exception,
            Model model
    ) {
        model.addAttribute("mensagemErro", exception.getMessage());
        return "erro";
    }

    @ExceptionHandler({
            TransactionSystemException.class,
            UnexpectedRollbackException.class
    })
    public String tratarErroDeTransacao(
            RuntimeException exception,
            Model model
    ) {
        Throwable causa = exception.getCause();

        while (causa != null
                && !(causa instanceof RegraNegocioException)
                && !(causa instanceof RecursoNaoEncontradoException)) {
            causa = causa.getCause();
        }

        if (causa instanceof RegraNegocioException
                || causa instanceof RecursoNaoEncontradoException) {
            model.addAttribute("mensagemErro", causa.getMessage());
            return "erro";
        }

        model.addAttribute(
                "mensagemErro",
                "Ocorreu um erro ao processar a operação."
        );

        return "erro";
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String tratarErroInesperado(
            RuntimeException exception,
            Model model
    ) {
        String mensagem = exception.getMessage();

        if (mensagem == null || mensagem.isBlank()) {
            mensagem = "Ocorreu um erro inesperado no sistema.";
        }

        model.addAttribute("mensagemErro", mensagem);
        return "erro";
    }
}
