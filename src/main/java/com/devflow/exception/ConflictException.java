package com.devflow.exception;

/** Conflito com o estado atual (ex.: e-mail já cadastrado) -> HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
