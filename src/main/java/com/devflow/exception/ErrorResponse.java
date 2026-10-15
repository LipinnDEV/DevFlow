package com.devflow.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

/** Formato padrão de todos os erros devolvidos pela API. */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDetail> errors
) {

    public record FieldErrorDetail(String field, String message) {
    }

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return of(status, message, path, List.of());
    }

    public static ErrorResponse of(HttpStatus status, String message, String path, List<FieldErrorDetail> errors) {
        return new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path, errors);
    }
}
