package com.devflow.service;

import com.devflow.dto.dashboard.DashboardResponse;
import com.devflow.dto.dashboard.UserProductivityResponse;
import com.devflow.entity.User;
import com.devflow.enums.ProjectStatus;
import com.devflow.enums.TaskPriority;
import com.devflow.enums.TaskStatus;
import com.devflow.repository.ProjectRepository;
import com.devflow.repository.TaskRepository;
import com.devflow.security.CurrentUserService;
import com.devflow.util.ProgressCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Estatísticas do dashboard: ADMIN vê o sistema todo; usuário comum vê só os projetos de que participa. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final CurrentUserService currentUserService;

    public DashboardResponse getDashboard() {
        User current = currentUserService.getCurrentUser();

        List<Long> projectIds = ProjectAccessService.isAdmin(current)
                ? projectRepository.findAllIds()
                : projectRepository.findIdsByMemberId(current.getId());

        if (projectIds.isEmpty()) {
            return DashboardResponse.empty();
        }

        long totalProjects = projectRepository.countByIdIn(projectIds);
        long completedProjects = projectRepository.countByIdInAndStatus(projectIds, ProjectStatus.CONCLUIDO);
        long inProgressProjects = projectRepository.countByIdInAndStatus(projectIds, ProjectStatus.EM_ANDAMENTO);

        long totalTasks = taskRepository.countByProjectIdIn(projectIds);
        long completedTasks = taskRepository.countByProjectIdInAndStatus(projectIds, TaskStatus.CONCLUIDA);
        long overdueTasks = taskRepository.countByProjectIdInAndDeadlineBeforeAndStatusNot(
                projectIds, LocalDate.now(), TaskStatus.CONCLUIDA);

        Map<TaskPriority, Long> byPriority = DashboardResponse.zeroedCounts(TaskPriority.class);
        for (Object[] row : taskRepository.countGroupedByPriority(projectIds)) {
            byPriority.put((TaskPriority) row[0], ((Number) row[1]).longValue());
        }

        Map<TaskStatus, Long> byStatus = DashboardResponse.zeroedCounts(TaskStatus.class);
        for (Object[] row : taskRepository.countGroupedByStatus(projectIds)) {
            byStatus.put((TaskStatus) row[0], ((Number) row[1]).longValue());
        }

        List<UserProductivityResponse> productivity = taskRepository
                .findProductivityByAssignee(projectIds, TaskStatus.CONCLUIDA).stream()
                .map(row -> {
                    long assigned = ((Number) row[2]).longValue();
                    long completed = ((Number) row[3]).longValue();
                    return new UserProductivityResponse((Long) row[0], (String) row[1], assigned, completed,
                            ProgressCalculator.percentage(assigned, completed));
                })
                .sorted(Comparator.comparingLong(UserProductivityResponse::completedTasks).reversed())
                .toList();

        return new DashboardResponse(totalProjects, completedProjects, inProgressProjects,
                totalTasks, completedTasks, overdueTasks, byPriority, byStatus, productivity);
    }
}
