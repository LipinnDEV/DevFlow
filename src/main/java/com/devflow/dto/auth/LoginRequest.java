package com.devflow.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "maria@email.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @Schema(example = "Senha1234")
        @NotBlank(message = "A senha é obrigatória")
        String password
) {
}
