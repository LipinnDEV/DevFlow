package com.devflow.dto.task;

import com.devflow.enums.TaskPriority;
import com.devflow.enums.TaskStatus;

/** Filtros opcionais (query string) para a listagem de tarefas. */
public record TaskFilter(
        TaskStatus status,
        TaskPriority priority,
        Long assigneeId,
        String tag,
        Boolean overdue,
        String search
) {
}
