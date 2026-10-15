package com.devflow.service;

import com.devflow.dto.task.TaskRequest;
import com.devflow.dto.task.TaskResponse;
import com.devflow.dto.task.UpdateTaskStatusRequest;
import com.devflow.entity.Project;
import com.devflow.entity.Tag;
import com.devflow.entity.Task;
import com.devflow.entity.User;
import com.devflow.enums.ActivityType;
import com.devflow.enums.ProjectStatus;
import com.devflow.enums.Role;
import com.devflow.enums.TaskPriority;
import com.devflow.enums.TaskStatus;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.ForbiddenOperationException;
import com.devflow.mapper.TaskMapper;
import com.devflow.mapper.UserMapper;
import com.devflow.repository.ProjectMemberRepository;
import com.devflow.repository.TagRepository;
import com.devflow.repository.TaskRepository;
import com.devflow.repository.UserRepository;
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
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private ProjectAccessService accessService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private ActivityService activityService;

    private TaskService taskService;

    private User owner;
    private User bruno;
    private Project project;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, projectMemberRepository, userRepository, tagRepository,
                accessService, currentUserService, activityService, new TaskMapper(new UserMapper()));

        owner = TestFactory.user(1L, "Ana", Role.USER);
        bruno = TestFactory.user(2L, "Bruno", Role.USER);
        project = TestFactory.project(1L, owner, LocalDate.now().plusDays(30));

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(accessService.getProjectForMember(1L, owner)).thenReturn(project);
        when(accessService.getProjectForMember(1L, bruno)).thenReturn(project);
        when(userRepository.findById(2L)).thenReturn(Optional.of(bruno));
        when(projectMemberRepository.existsByProjectIdAndUserId(1L, 2L)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------------- criação ----------------

    @Test
    @DisplayName("Criar tarefa: valores padrão, responsável, tags normalizadas e atividade registrada")
    void create_sucesso() {
        TaskRequest request = new TaskRequest("Criar login", "Tela de login", null, TaskPriority.ALTA,
                2L, LocalDate.now().plusDays(5), Set.of("Backend", "API"));

        TaskResponse response = taskService.create(1L, request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.priority()).isEqualTo(TaskPriority.ALTA);
        assertThat(response.assignee().id()).isEqualTo(2L);
        assertThat(response.projectId()).isEqualTo(1L);
        assertThat(response.tags()).containsExactly("api", "backend");
        assertThat(response.overdue()).isFalse();
        verify(activityService).register(eq(ActivityType.TAREFA_CRIADA), anyString(), eq(owner), eq(1L), eq(100L));
    }

    @Test
    @DisplayName("Criar tarefa: sem prioridade informada assume MEDIA")
    void create_prioridadePadrao() {
        TaskResponse response = taskService.create(1L, new TaskRequest("Tarefa", null, null, null, null, null, null));

        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIA);
        assertThat(response.assignee()).isNull();
    }

    @Test
    @DisplayName("Criar tarefa: o responsável precisa ser membro do projeto")
    void create_responsavelNaoMembro() {
        User carla = TestFactory.user(3L, "Carla", Role.USER);
        when(userRepository.findById(3L)).thenReturn(Optional.of(carla));
        when(projectMemberRepository.existsByProjectIdAndUserId(1L, 3L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.create(1L,
                new TaskRequest("Tarefa", null, null, null, 3L, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("membro");

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criar tarefa: prazo no passado deve ser rejeitado")
    void create_prazoNoPassado() {
        assertThatThrownBy(() -> taskService.create(1L,
                new TaskRequest("Tarefa", null, null, null, null, LocalDate.now().minusDays(1), null)))
                .isInstanceOf(BusinessRuleException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criar tarefa: prazo posterior ao prazo do projeto deve ser rejeitado")
    void create_prazoPosteriorAoProjeto() {
        project.setDeadline(LocalDate.now().plusDays(10));

        assertThatThrownBy(() -> taskService.create(1L,
                new TaskRequest("Tarefa", null, null, null, null, LocalDate.now().plusDays(20), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("projeto");

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criar tarefa já concluída sem responsável deve ser rejeitado")
    void create_concluidaSemResponsavel() {
        assertThatThrownBy(() -> taskService.create(1L,
                new TaskRequest("Tarefa", null, TaskStatus.CONCLUIDA, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class);

        verify(taskRepository, never()).save(any());
    }

    // ---------------- conclusão ----------------

    @Test
    @DisplayName("Concluir tarefa sem responsável deve lançar BusinessRuleException")
    void updateStatus_concluirSemResponsavel() {
        Task task = TestFactory.task(100L, project, owner, null, TaskStatus.EM_REVISAO);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateStatus(100L, new UpdateTaskStatusRequest(TaskStatus.CONCLUIDA)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("sem responsável");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.EM_REVISAO);
        assertThat(task.getCompletedAt()).isNull();
        verifyNoInteractions(activityService);
    }

    @Test
    @DisplayName("Concluir tarefa com responsável registra a data de conclusão e a atividade")
    void updateStatus_concluirComResponsavel() {
        Task task = TestFactory.task(100L, project, owner, bruno, TaskStatus.EM_REVISAO);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(taskRepository.countByProjectId(1L)).thenReturn(2L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.CONCLUIDA)).thenReturn(1L);

        TaskResponse response = taskService.updateStatus(100L, new UpdateTaskStatusRequest(TaskStatus.CONCLUIDA));

        assertThat(response.status()).isEqualTo(TaskStatus.CONCLUIDA);
        assertThat(response.completedAt()).isNotNull();
        assertThat(project.getStatus()).isNotEqualTo(ProjectStatus.CONCLUIDO);
        verify(activityService).register(eq(ActivityType.TAREFA_CONCLUIDA), anyString(), eq(owner), eq(1L), eq(100L));
    }

    @Test
    @DisplayName("Concluir a última tarefa conclui automaticamente o projeto")
    void updateStatus_ultimaTarefaConcluiProjeto() {
        Task task = TestFactory.task(100L, project, owner, bruno, TaskStatus.EM_ANDAMENTO);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(taskRepository.countByProjectId(1L)).thenReturn(1L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.CONCLUIDA)).thenReturn(1L);

        taskService.updateStatus(100L, new UpdateTaskStatusRequest(TaskStatus.CONCLUIDA));

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.CONCLUIDO);
        verify(activityService).register(eq(ActivityType.PROJETO_CONCLUIDO), anyString(), eq(owner), eq(1L), isNull());
    }

    @Test
    @DisplayName("Reabrir tarefa limpa a data de conclusão e reabre o projeto")
    void updateStatus_reabrirTarefa() {
        project.setStatus(ProjectStatus.CONCLUIDO);
        Task task = TestFactory.task(100L, project, owner, bruno, TaskStatus.CONCLUIDA);
        task.setCompletedAt(LocalDateTime.now());
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(taskRepository.countByProjectId(1L)).thenReturn(1L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.CONCLUIDA)).thenReturn(0L);

        TaskResponse response = taskService.updateStatus(100L, new UpdateTaskStatusRequest(TaskStatus.EM_ANDAMENTO));

        assertThat(response.completedAt()).isNull();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.EM_ANDAMENTO);
        verify(activityService).register(eq(ActivityType.STATUS_ALTERADO), anyString(), eq(owner), eq(1L), eq(100L));
    }

    @Test
    @DisplayName("Atualizar tarefa concluída: não pode remover o responsável")
    void update_removerResponsavelDeTarefaConcluida() {
        Task task = TestFactory.task(100L, project, owner, bruno, TaskStatus.CONCLUIDA);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.update(100L,
                new TaskRequest("Tarefa 100", null, null, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ---------------- exclusão ----------------

    @Test
    @DisplayName("Excluir tarefa: membro que não é dono do projeto nem criador da tarefa não pode excluir")
    void delete_semPermissao() {
        Task task = TestFactory.task(100L, project, owner, null, TaskStatus.TODO);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(currentUserService.getCurrentUser()).thenReturn(bruno);

        assertThatThrownBy(() -> taskService.delete(100L))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    @DisplayName("Excluir tarefa: proprietário do projeto pode excluir")
    void delete_proprietarioDoProjeto() {
        Task task = TestFactory.task(100L, project, bruno, null, TaskStatus.TODO);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

        taskService.delete(100L);

        verify(taskRepository).delete(task);
        verify(activityService).register(eq(ActivityType.TAREFA_EXCLUIDA), anyString(), eq(owner), eq(1L), eq(100L));
    }

    @Test
    @DisplayName("Excluir tarefa: criador da tarefa pode excluir")
    void delete_criadorDaTarefa() {
        Task task = TestFactory.task(100L, project, bruno, null, TaskStatus.TODO);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(currentUserService.getCurrentUser()).thenReturn(bruno);

        taskService.delete(100L);

        verify(taskRepository).delete(task);
    }
}
