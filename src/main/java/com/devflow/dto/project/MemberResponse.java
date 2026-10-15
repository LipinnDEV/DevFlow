package com.devflow.dto.project;

import com.devflow.enums.ProjectMemberRole;

import java.time.LocalDateTime;

public record MemberResponse(
        Long userId,
        String name,
        String email,
        ProjectMemberRole role,
        LocalDateTime joinedAt
) {
}
