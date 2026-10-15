package com.devflow.dto.task;

import com.devflow.dto.common.UserSummary;
import com.devflow.enums.TaskPriority;
import com.devflow.enums.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        UserSummary assignee,
        UserSummary createdBy,
        Long projectId,
        LocalDate deadline,
        boolean overdue,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt,
        List<String> tags
) {
}
