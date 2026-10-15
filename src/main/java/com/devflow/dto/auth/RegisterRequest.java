package com.devflow.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "Maria Silva")
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
        String name,

        @Schema(example = "maria@email.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        String email,

        @Schema(example = "Senha1234")
        @NotBlank(message = "A senha é obrigatória")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$",
                message = "A senha deve ter de 8 a 64 caracteres, com letras e números")
        String password
) {
}
