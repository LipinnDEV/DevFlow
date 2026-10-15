package com.devflow.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "O comentário não pode ser vazio")
        @Size(max = 2000, message = "O comentário deve ter no máximo 2000 caracteres")
        String text
) {
}
