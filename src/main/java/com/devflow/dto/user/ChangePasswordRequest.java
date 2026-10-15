package com.devflow.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @NotBlank(message = "A senha atual é obrigatória")
        String currentPassword,

        @NotBlank(message = "A nova senha é obrigatória")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$",
                message = "A senha deve ter de 8 a 64 caracteres, com letras e números")
        String newPassword
) {
}
