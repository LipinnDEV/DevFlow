package com.devflow.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AddMemberRequest(
        @Schema(example = "colega@email.com")
        @NotBlank(message = "O e-mail do usuário é obrigatório")
        @Email(message = "E-mail inválido")
        String email
) {
}
