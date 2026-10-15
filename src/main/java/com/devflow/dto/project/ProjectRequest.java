package com.devflow.dto.project;

import com.devflow.enums.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProjectRequest(
        @Schema(example = "Aplicativo mobile")
        @NotBlank(message = "O nome do projeto é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String name,

        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
        String description,

        @Schema(description = "Se omitido na criação, o projeto começa como PLANEJADO")
        ProjectStatus status,

        @Schema(example = "2026-12-31")
        LocalDate deadline
) {
}
