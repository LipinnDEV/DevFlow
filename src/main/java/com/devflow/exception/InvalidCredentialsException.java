package com.devflow.exception;

/** Credenciais inválidas ou usuário não autenticado -> HTTP 401. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("E-mail ou senha inválidos");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
