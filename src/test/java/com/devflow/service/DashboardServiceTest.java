package com.devflow.service;

import com.devflow.dto.dashboard.DashboardResponse;
import com.devflow.entity.User;
import com.devflow.enums.ProjectStatus;
import com.devflow.enums.Role;
import com.devflow.enums.TaskPriority;
import com.devflow.enums.TaskStatus;
import com.devflow.repository.ProjectRepository;
import com.devflow.repository.TaskRepository;
import com.devflow.security.CurrentUserService;
import com.devflow.support.TestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private CurrentUserService currentUserService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(projectRepository, taskRepository, currentUserService);
    }

    @Test
    @DisplayName("Dashboard: agrega totais, atrasadas, prioridades, status e produtividade")
    void deveAgregarEstatisticas() {
        User ana = TestFactory.user(1L, "Ana", Role.USER);
        List<Long> ids = List.of(1L, 2L);

        when(currentUserService.getCurrentUser()).thenReturn(ana);
        when(projectRepository.findIdsByMemberId(1L)).thenReturn(ids);
        when(projectRepository.countByIdIn(ids)).thenReturn(2L);
        when(projectRepository.countByIdInAndStatus(ids, ProjectStatus.CONCLUIDO)).thenReturn(1L);
        when(projectRepository.countByIdInAndStatus(ids, ProjectStatus.EM_ANDAMENTO)).thenReturn(1L);
        when(taskRepository.countByProjectIdIn(ids)).thenReturn(10L);
        when(taskRepository.countByProjectIdInAndStatus(ids, TaskStatus.CONCLUIDA)).thenReturn(4L);
        when(taskRepository.countByProjectIdInAndDeadlineBeforeAndStatusNot(
                eq(ids), any(LocalDate.class), eq(TaskStatus.CONCLUIDA))).thenReturn(2L);
        when(taskRepository.countGroupedByPriority(ids)).thenReturn(List.<Object[]>of(
                new Object[]{TaskPriority.ALTA, 3L}, new Object[]{TaskPriority.BAIXA, 7L}));
        when(taskRepository.countGroupedByStatus(ids)).thenReturn(List.<Object[]>of(
                new Object[]{TaskStatus.TODO, 6L}, new Object[]{TaskStatus.CONCLUIDA, 4L}));
        when(taskRepository.findProductivityByAssignee(ids, TaskStatus.CONCLUIDA)).thenReturn(List.<Object[]>of(
                new Object[]{2L, "Bruno", 2L, 1L}, new Object[]{1L, "Ana", 4L, 3L}));

        DashboardResponse response = dashboardService.getDashboard();

        assertThat(response.totalProjects()).isEqualTo(2);
        assertThat(response.completedProjects()).isEqualTo(1);
        assertThat(response.inProgressProjects()).isEqualTo(1);
        assertThat(response.totalTasks()).isEqualTo(10);
        assertThat(response.completedTasks()).isEqualTo(4);
        assertThat(response.overdueTasks()).isEqualTo(2);

        assertThat(response.tasksByPriority().get(TaskPriority.ALTA)).isEqualTo(3L);
        assertThat(response.tasksByPriority().get(TaskPriority.BAIXA)).isEqualTo(7L);
        assertThat(response.tasksByPriority().get(TaskPriority.URGENTE)).isZero(); // categoria sem tarefas aparece zerada
        assertThat(response.tasksByStatus().get(TaskStatus.TODO)).isEqualTo(6L);
        assertThat(response.tasksByStatus().get(TaskStatus.EM_REVISAO)).isZero();

        // ordenado por tarefas concluídas (maior primeiro)
        assertThat(response.userProductivity()).hasSize(2);
        assertThat(response.userProductivity().get(0).name()).isEqualTo("Ana");
        assertThat(response.userProductivity().get(0).completionRate()).isEqualTo(75.0);
        assertThat(response.userProductivity().get(1).name()).isEqualTo("Bruno");
        assertThat(response.userProductivity().get(1).completionRate()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Dashboard: usuário sem projetos recebe tudo zerado, sem consultar tarefas")
    void usuarioSemProjetos() {
        User ana = TestFactory.user(1L, "Ana", Role.USER);
        when(currentUserService.getCurrentUser()).thenReturn(ana);
        when(projectRepository.findIdsByMemberId(1L)).thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard();

        assertThat(response.totalProjects()).isZero();
        assertThat(response.totalTasks()).isZero();
        assertThat(response.tasksByStatus()).hasSize(TaskStatus.values().length);
        assertThat(response.userProductivity()).isEmpty();
        verifyNoInteractions(taskRepository);
    }

    @Test
    @DisplayName("Dashboard: administrador enxerga todos os projetos")
    void administradorVeTodosOsProjetos() {
        User admin = TestFactory.user(9L, "Admin", Role.ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(projectRepository.findAllIds()).thenReturn(List.of());

        dashboardService.getDashboard();

        verify(projectRepository).findAllIds();
        verify(projectRepository, never()).findIdsByMemberId(any());
    }
}
