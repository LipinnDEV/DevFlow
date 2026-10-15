package com.devflow.mapper;

import com.devflow.dto.task.TaskResponse;
import com.devflow.entity.Tag;
import com.devflow.entity.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskMapper {

    private final UserMapper userMapper;

    public TaskResponse toResponse(Task task) {
        List<String> tags = task.getTags().stream()
                .map(Tag::getName)
                .sorted()
                .toList();

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                userMapper.toSummary(task.getAssignee()),
                userMapper.toSummary(task.getCreatedBy()),
                task.getProject().getId(),
                task.getDeadline(),
                task.isOverdue(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getCompletedAt(),
                tags);
    }
}
