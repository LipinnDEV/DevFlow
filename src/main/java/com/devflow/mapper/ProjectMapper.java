package com.devflow.mapper;

import com.devflow.dto.project.MemberResponse;
import com.devflow.dto.project.ProjectResponse;
import com.devflow.entity.Project;
import com.devflow.entity.ProjectMember;
import com.devflow.util.ProgressCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectMapper {

    private final UserMapper userMapper;

    public ProjectResponse toResponse(Project project, long memberCount, long totalTasks, long completedTasks) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getDeadline(),
                userMapper.toSummary(project.getOwner()),
                memberCount,
                totalTasks,
                completedTasks,
                ProgressCalculator.percentage(totalTasks, completedTasks),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }

    public MemberResponse toMemberResponse(ProjectMember member) {
        return new MemberResponse(
                member.getUser().getId(),
                member.getUser().getName(),
                member.getUser().getEmail(),
                member.getRole(),
                member.getJoinedAt());
    }
}
