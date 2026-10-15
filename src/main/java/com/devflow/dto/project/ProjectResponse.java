package com.devflow.dto.project;

import com.devflow.dto.common.UserSummary;
import com.devflow.enums.ProjectStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        ProjectStatus status,
        LocalDate deadline,
        UserSummary owner,
        long memberCount,
        long totalTasks,
        long completedTasks,
        double completionPercentage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
