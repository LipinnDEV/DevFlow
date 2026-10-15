package com.devflow.exception;

/** Usuário autenticado sem permissão para a operação -> HTTP 403. */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
