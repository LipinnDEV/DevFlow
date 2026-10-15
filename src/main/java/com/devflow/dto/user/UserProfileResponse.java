package com.devflow.dto.user;

import com.devflow.enums.Role;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String name,
        String email,
        Role role,
        LocalDateTime createdAt,
        long projectsCount,
        long assignedTasks,
        long completedTasks
) {
}
