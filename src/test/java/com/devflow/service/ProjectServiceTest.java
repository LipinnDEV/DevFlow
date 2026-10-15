package com.devflow.service;

import com.devflow.dto.project.AddMemberRequest;
import com.devflow.dto.project.MemberResponse;
import com.devflow.dto.project.ProjectRequest;
import com.devflow.dto.project.ProjectResponse;
import com.devflow.dto.project.TransferOwnershipRequest;
import com.devflow.entity.Project;
import com.devflow.entity.User;
import com.devflow.enums.ActivityType;
import com.devflow.enums.ProjectMemberRole;
import com.devflow.enums.ProjectStatus;
import com.devflow.enums.Role;
import com.devflow.enums.TaskStatus;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.ConflictException;
import com.devflow.exception.ForbiddenOperationException;
import com.devflow.mapper.ProjectMapper;
import com.devflow.mapper.UserMapper;
import com.devflow.repository.ProjectMemberRepository;
import com.devflow.repository.ProjectRepository;
import com.devflow.repository.TaskRepository;
import com.devflow.repository.UserRepository;
import com.devflow.security.CurrentUserService;
import com.devflow.support.TestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectAccessService accessService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private ActivityService activityService;

    private ProjectService projectService;
    private User owner;
    private User bruno;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository, projectMemberRepository, taskRepository,
                userRepository, accessService, currentUserService, activityService,
                new ProjectMapper(new UserMapper()));
        owner = TestFactory.user(1L, "Ana", Role.USER);
        bruno = TestFactory.user(2L, "Bruno", Role.USER);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
    }

    // ---------------- criação ----------------

    @Test
    @DisplayName("Criar projeto: criador vira proprietário e membro, e a atividade é registrada")
    void create_defineProprietarioComoMembro() {
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        ProjectResponse response = projectService.create(
                new ProjectRequest("DevFlow", "API de projetos", null, LocalDate.now().plusDays(10)));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(ProjectStatus.PLANEJADO);
        assertThat(response.owner().id()).isEqualTo(1L);
        assertThat(response.completionPercentage()).isZero();

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        Project saved = captor.getValue();
        assertThat(saved.getMembers()).hasSize(1);
        assertThat(saved.getMembers().get(0).getRole()).isEqualTo(ProjectMemberRole.OWNER);
        assertThat(saved.getMembers().get(0).getUser()).isSameAs(owner);

        verify(activityService).register(eq(ActivityType.PROJETO_CRIADO), contains("DevFlow"),
                eq(owner), eq(10L), isNull());
    }

    @Test
    @DisplayName("Criar projeto: prazo no passado deve ser rejeitado")
    void create_prazoNoPassado() {
        ProjectRequest request = new ProjectRequest("DevFlow", null, null, LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> projectService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("prazo");

        verify(projectRepository, never()).save(any());
    }

    // ---------------- consulta / progresso ----------------

    @Test
    @DisplayName("Consultar projeto: percentual de conclusão é calculado a partir das tarefas")
    void getById_calculaPercentual() {
        Project project = TestFactory.project(1L, owner, null);
        when(accessService.getProjectForMember(1L, owner)).thenReturn(project);
        when(taskRepository.countByProjectId(1L)).thenReturn(4L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.CONCLUIDA)).thenReturn(1L);
        when(projectMemberRepository.countByProjectId(1L)).thenReturn(2L);

        ProjectResponse response = projectService.getById(1L);

        assertThat(response.totalTasks()).isEqualTo(4);
        assertThat(response.completedTasks()).isEqualTo(1);
        assertThat(response.completionPercentage()).isEqualTo(25.0);
        assertThat(response.memberCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Consultar projeto: projeto sem tarefas tem 0% de conclusão")
    void getById_semTarefas() {
        Project project = TestFactory.project(1L, owner, null);
        when(accessService.getProjectForMember(1L, owner)).thenReturn(project);

        assertThat(projectService.getById(1L).completionPercentage()).isZero();
    }

    // ---------------- atualização ----------------

    @Test
    @DisplayName("Atualizar projeto: não pode concluir com tarefas abertas")
    void update_concluirComTarefasAbertas() {
        Project project = TestFactory.project(1L, owner, null);
        project.setStatus(ProjectStatus.EM_ANDAMENTO);
        when(accessService.getProjectForMember(1L, owner)).thenReturn(project);
        when(taskRepository.countByProjectIdAndStatusNot(1L, TaskStatus.CONCLUIDA)).thenReturn(2L);

        assertThatThrownBy(() -> projectService.update(1L,
                new ProjectRequest("Projeto 1", null, ProjectStatus.CONCLUIDO, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("tarefas");

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.EM_ANDAMENTO);
    }

    @Test
    @DisplayName("Atualizar projeto: conclui quando todas as tarefas estão concluídas")
    void update_concluirSemTarefasAbertas() {
        Project project = TestFactory.project(1L, owner, null);
        project.setStatus(ProjectStatus.EM_ANDAMENTO);
        when(accessService.getProjectForMember(1L, owner)).thenReturn(project);
        when(taskRepository.countByProjectIdAndStatusNot(1L, TaskStatus.CONCLUIDA)).thenReturn(0L);

        ProjectResponse response = projectService.update(1L,
                new ProjectRequest("Projeto 1", "novo texto", ProjectStatus.CONCLUIDO, null));

        assertThat(response.status()).isEqualTo(ProjectStatus.CONCLUIDO);
        assertThat(project.getDescription()).isEqualTo("novo texto");
        verify(activityService).register(eq(ActivityType.PROJETO_CONCLUIDO), anyString(), eq(owner), eq(1L), isNull());
    }

    @Test
    @DisplayName("Atualizar projeto: quem não participa não pode editar")
    void update_semPermissao() {
        when(accessService.getProjectForMember(1L, owner)).thenThrow(new ForbiddenOperationException("Você não participa deste projeto"));

        assertThatThrownBy(() -> projectService.update(1L, new ProjectRequest("X", null, null, null)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    // ---------------- exclusão ----------------

    @Test
    @DisplayName("Excluir projeto: só o proprietário pode excluir")
    void delete_semPermissao() {
        when(accessService.getProjectForOwner(1L, owner)).thenThrow(new ForbiddenOperationException("Apenas o proprietário"));

        assertThatThrownBy(() -> projectService.delete(1L))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(projectRepository, never()).delete(any(Project.class));
    }

    @Test
    @DisplayName("Excluir projeto: proprietário exclui e a atividade é registrada")
    void delete_proprietario() {
        Project project = TestFactory.project(1L, owner, null);
        when(accessService.getProjectForOwner(1L, owner)).thenReturn(project);

        projectService.delete(1L);

        verify(projectRepository).delete(project);
        verify(activityService).register(eq(ActivityType.PROJETO_EXCLUIDO), anyString(), eq(owner), eq(1L), isNull());
    }

    // ---------------- membros ----------------

    @Test
    @DisplayName("Adicionar membro: o mesmo usuário não pode entrar duas vezes")
    void addMember_duplicado() {
        Project project = TestFactory.project(1L, owner, null);
        when(accessService.getProjectForOwner(1L, owner)).thenReturn(project);
        when(userRepository.findByEmail("bruno@test.com")).thenReturn(Optional.of(bruno));
        when(projectMemberRepository.existsByProjectIdAndUserId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> projectService.addMember(1L, new AddMemberRequest("Bruno@test.com")))
                .isInstanceOf(ConflictException.class);

        assertThat(project.getMembers()).isEmpty();
    }

    @Test
    @DisplayName("Adicionar membro: usuário desativado não pode ser adicionado")
    void addMember_usuarioDesativado() {
        bruno.setActive(false);
        Project project = TestFactory.project(1L, owner, null);
        when(accessService.getProjectForOwner(1L, owner)).thenReturn(project);
        when(userRepository.findByEmail("bruno@test.com")).thenReturn(Optional.of(bruno));

        assertThatThrownBy(() -> projectService.addMember(1L, new AddMemberRequest("bruno@test.com")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Adicionar membro: deve adicionar como MEMBER e registrar a atividade")
    void addMember_sucesso() {
        Project project = TestFactory.project(1L, owner, null);
        when(accessService.getProjectForOwner(1L, owner)).thenReturn(project);
        when(userRepository.findByEmail("bruno@test.com")).thenReturn(Optional.of(bruno));
        when(projectMemberRepository.existsByProjectIdAndUserId(1L, 2L)).thenReturn(false);

        MemberResponse response = projectService.addMember(1L, new AddMemberRequest("bruno@test.com"));

        assertThat(response.userId()).isEqualTo(2L);
        assertThat(response.role()).isEqualTo(ProjectMemberRole.MEMBER);
        assertThat(project.getMembers()).hasSize(1);
        verify(projectRepository).flush();
        verify(activityService).register(eq(ActivityType.MEMBRO_ADICIONADO), anyString(), eq(owner), eq(1L), isNull());
    }

    @Test
    @DisplayName("Remover membro: o proprietário não pode ser removido")
    void removeMember_proprietario() {
        Project project = TestFactory.project(1L, owner, null);
        when(accessService.getProjectForOwner(1L, owner)).thenReturn(project);

        assertThatThrownBy(() -> projectService.removeMember(1L, 1L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Remover membro: remove e deixa as tarefas dele sem responsável")
    void removeMember_sucesso() {
        Project project = TestFactory.project(1L, owner, null);
        project.addMember(owner, ProjectMemberRole.OWNER);
        project.addMember(bruno, ProjectMemberRole.MEMBER);
        when(accessService.getProjectForOwner(1L, owner)).thenReturn(project);

        projectService.removeMember(1L, 2L);

        assertThat(project.getMembers()).hasSize(1);
        verify(taskRepository).unassignUserFromProject(1L, 2L);
        verify(activityService).register(eq(ActivityType.MEMBRO_REMOVIDO), anyString(), eq(owner), eq(1L), isNull());
    }

    @Test
    @DisplayName("Transferir propriedade: novo proprietário precisa ser membro")
    void transferOwnership_naoMembro() {
        Project project = TestFactory.project(1L, owner, null);
        project.addMember(owner, ProjectMemberRole.OWNER);
        when(accessService.getProjectForOwner(1L, owner)).thenReturn(project);

        assertThatThrownBy(() -> projectService.transferOwnership(1L, new TransferOwnershipRequest(2L)))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(project.getOwner()).isSameAs(owner);
    }

    @Test
    @DisplayName("Transferir propriedade: troca o proprietário e os papéis dos membros")
    void transferOwnership_sucesso() {
        Project project = TestFactory.project(1L, owner, null);
        project.addMember(owner, ProjectMemberRole.OWNER);
        project.addMember(bruno, ProjectMemberRole.MEMBER);
        when(accessService.getProjectForOwner(1L, owner)).thenReturn(project);

        ProjectResponse response = projectService.transferOwnership(1L, new TransferOwnershipRequest(2L));

        assertThat(response.owner().id()).isEqualTo(2L);
        assertThat(project.getMembers().get(0).getRole()).isEqualTo(ProjectMemberRole.MEMBER);
        assertThat(project.getMembers().get(1).getRole()).isEqualTo(ProjectMemberRole.OWNER);
        verify(activityService).register(eq(ActivityType.PROPRIETARIO_ALTERADO), anyString(), eq(owner), eq(1L), isNull());
    }
}
