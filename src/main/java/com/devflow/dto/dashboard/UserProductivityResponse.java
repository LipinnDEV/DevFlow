package com.devflow.dto.dashboard;

public record UserProductivityResponse(
        Long userId,
        String name,
        long assignedTasks,
        long completedTasks,
        double completionRate
) {
}
