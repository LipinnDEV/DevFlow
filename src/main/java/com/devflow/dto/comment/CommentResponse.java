package com.devflow.dto.comment;

import com.devflow.dto.common.UserSummary;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        String text,
        UserSummary author,
        Long taskId,
        LocalDateTime createdAt
) {
}
