package com.devflow.dto.task;

import com.devflow.enums.TaskPriority;
import com.devflow.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

/** Usado para criar (POST) e atualizar (PUT) tarefas. */
public record TaskRequest(
        @Schema(example = "Implementar tela de login")
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 200, message = "O título deve ter no máximo 200 caracteres")
        String title,

        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
        String description,

        @Schema(description = "Se omitido na criação: TODO. Se omitido na atualização: mantém o atual")
        TaskStatus status,

        @Schema(description = "Se omitido na criação: MEDIA. Se omitido na atualização: mantém a atual")
        TaskPriority priority,

        @Schema(description = "Id do responsável (precisa ser membro do projeto). Nulo = sem responsável")
        Long assigneeId,

        @Schema(example = "2026-11-30")
        LocalDate deadline,

        @Schema(example = "[\"backend\", \"api\"]")
        Set<@NotBlank(message = "A tag não pode ser vazia")
        @Size(max = 30, message = "Cada tag deve ter no máximo 30 caracteres") String> tags
) {
}
