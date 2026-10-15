package com.devflow.exception;

/** Violação de regra de negócio -> HTTP 422. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
