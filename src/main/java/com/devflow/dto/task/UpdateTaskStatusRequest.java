package com.devflow.dto.task;

import com.devflow.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
        @NotNull(message = "O status é obrigatório")
        TaskStatus status
) {
}
