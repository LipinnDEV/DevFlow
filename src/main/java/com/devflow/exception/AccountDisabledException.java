package com.devflow.exception;

/** Tentativa de login com conta desativada -> HTTP 403. */
public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException() {
        super("Esta conta está desativada");
    }
}
