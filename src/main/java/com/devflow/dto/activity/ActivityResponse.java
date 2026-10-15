package com.devflow.dto.activity;

import com.devflow.dto.common.UserSummary;
import com.devflow.enums.ActivityType;

import java.time.LocalDateTime;

public record ActivityResponse(
        Long id,
        ActivityType type,
        String description,
        UserSummary user,
        Long projectId,
        Long taskId,
        LocalDateTime createdAt
) {
}
