package com.devflow.service;

import com.devflow.dto.common.PageResponse;
import com.devflow.dto.project.AddMemberRequest;
import com.devflow.dto.project.MemberResponse;
import com.devflow.dto.project.ProjectRequest;
import com.devflow.dto.project.ProjectResponse;
import com.devflow.dto.project.TransferOwnershipRequest;
import com.devflow.entity.Project;
import com.devflow.entity.ProjectMember;
import com.devflow.entity.User;
import com.devflow.enums.ActivityType;
import com.devflow.enums.ProjectMemberRole;
import com.devflow.enums.ProjectStatus;
import com.devflow.enums.TaskStatus;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.ConflictException;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.mapper.ProjectMapper;
import com.devflow.repository.ProjectMemberRepository;
import com.devflow.repository.ProjectRepository;
import com.devflow.repository.TaskRepository;
import com.devflow.repository.UserRepository;
import com.devflow.security.CurrentUserService;
import com.devflow.specification.ProjectSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService accessService;
    private final CurrentUserService currentUserService;
    private final ActivityService activityService;
    private final ProjectMapper projectMapper;

    public ProjectResponse create(ProjectRequest request) {
        User current = currentUserService.getCurrentUser();
        validateDeadline(request.deadline(), null);

        Project project = Project.builder()
                .name(request.name().trim())
                .description(request.description())
                .status(request.status() != null ? request.status() : ProjectStatus.PLANEJADO)
                .deadline(request.deadline())
                .owner(current)
                .build();
        // O criador é o proprietário e também aparece na lista de membros
        project.addMember(current, ProjectMemberRole.OWNER);

        Project saved = projectRepository.save(project);
        activityService.register(ActivityType.PROJETO_CRIADO,
                current.getName() + " criou o projeto \"" + saved.getName() + "\"",
                current, saved.getId(), null);
        log.info("Projeto {} criado por {}", saved.getId(), current.getEmail());
        return toResponse(saved);
    }

    /** ADMIN vê todos os projetos; usuário comum vê apenas aqueles de que participa. */
    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> list(ProjectStatus status, String name, Pageable pageable) {
        User current = currentUserService.getCurrentUser();

        Specification<Project> spec = Specification.where(ProjectSpecifications.hasStatus(status))
                .and(ProjectSpecifications.nameContains(name));
        if (!ProjectAccessService.isAdmin(current)) {
            spec = spec.and(ProjectSpecifications.hasMember(current.getId()));
        }

        Page<ProjectResponse> page = projectRepository.findAll(spec, pageable).map(this::toResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        User current = currentUserService.getCurrentUser();
        Project project = accessService.getProjectForMember(id, current);
        return toResponse(project);
    }

    public ProjectResponse update(Long id, ProjectRequest request) {
        User current = currentUserService.getCurrentUser();
        // Regra: só edita quem participa do projeto (ou é proprietário/admin)
        Project project = accessService.getProjectForMember(id, current);

        validateDeadline(request.deadline(), project.getDeadline());

        boolean becameCompleted = false;
        if (request.status() != null && request.status() != project.getStatus()) {
            if (request.status() == ProjectStatus.CONCLUIDO) {
                long openTasks = taskRepository.countByProjectIdAndStatusNot(id, TaskStatus.CONCLUIDA);
                if (openTasks > 0) {
                    throw new BusinessRuleException(
                            "O projeto só pode ser concluído quando todas as tarefas estiverem concluídas ("
                                    + openTasks + " pendente(s))");
                }
                becameCompleted = true;
            }
            project.setStatus(request.status());
        }

        project.setName(request.name().trim());
        project.setDescription(request.description());
        project.setDeadline(request.deadline());

        ActivityType type = becameCompleted ? ActivityType.PROJETO_CONCLUIDO : ActivityType.PROJETO_ATUALIZADO;
        String action = becameCompleted ? " concluiu o projeto \"" : " atualizou o projeto \"";
        activityService.register(type, current.getName() + action + project.getName() + "\"",
                current, project.getId(), null);
        return toResponse(project);
    }

    public void delete(Long id) {
        User current = currentUserService.getCurrentUser();
        // Regra: só o proprietário (ou ADMIN) exclui
        Project project = accessService.getProjectForOwner(id, current);

        activityService.register(ActivityType.PROJETO_EXCLUIDO,
                current.getName() + " excluiu o projeto \"" + project.getName() + "\"",
                current, id, null);
        projectRepository.delete(project); // tarefas, comentários e membros saem em cascata
        log.info("Projeto {} excluído por {}", id, current.getEmail());
    }

    // ---------- membros ----------

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long projectId) {
        User current = currentUserService.getCurrentUser();
        accessService.getProjectForMember(projectId, current);
        return projectMemberRepository.findByProjectId(projectId).stream()
                .map(projectMapper::toMemberResponse)
                .toList();
    }

    public MemberResponse addMember(Long projectId, AddMemberRequest request) {
        User current = currentUserService.getCurrentUser();
        Project project = accessService.getProjectForOwner(projectId, current);

        User newMember = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum usuário encontrado com este e-mail"));

        if (!newMember.isActive()) {
            throw new BusinessRuleException("Não é possível adicionar um usuário com a conta desativada");
        }
        // Regra: o mesmo usuário não entra duas vezes (o banco também garante com uma constraint única)
        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, newMember.getId())) {
            throw new ConflictException("O usuário já participa deste projeto");
        }

        ProjectMember member = project.addMember(newMember, ProjectMemberRole.MEMBER);
        projectRepository.flush();

        activityService.register(ActivityType.MEMBRO_ADICIONADO,
                current.getName() + " adicionou " + newMember.getName() + " ao projeto \"" + project.getName() + "\"",
                current, projectId, null);
        return projectMapper.toMemberResponse(member);
    }

    public void removeMember(Long projectId, Long userId) {
        User current = currentUserService.getCurrentUser();
        Project project = accessService.getProjectForOwner(projectId, current);

        if (project.getOwner().getId().equals(userId)) {
            throw new BusinessRuleException("O proprietário não pode ser removido do projeto");
        }

        ProjectMember member = project.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Este usuário não é membro do projeto"));

        project.getMembers().remove(member); // orphanRemoval apaga a linha em project_members
        projectRepository.flush();
        // As tarefas do membro removido ficam sem responsável
        taskRepository.unassignUserFromProject(projectId, userId);

        activityService.register(ActivityType.MEMBRO_REMOVIDO,
                current.getName() + " removeu " + member.getUser().getName() + " do projeto \"" + project.getName() + "\"",
                current, projectId, null);
    }

    public ProjectResponse transferOwnership(Long projectId, TransferOwnershipRequest request) {
        User current = currentUserService.getCurrentUser();
        Project project = accessService.getProjectForOwner(projectId, current);

        if (project.getOwner().getId().equals(request.newOwnerId())) {
            throw new BusinessRuleException("Este usuário já é o proprietário do projeto");
        }

        ProjectMember newOwner = project.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(request.newOwnerId()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("O novo proprietário precisa ser membro do projeto"));

        project.getMembers().forEach(m -> m.setRole(
                m.getUser().getId().equals(request.newOwnerId()) ? ProjectMemberRole.OWNER : ProjectMemberRole.MEMBER));
        project.setOwner(newOwner.getUser());

        activityService.register(ActivityType.PROPRIETARIO_ALTERADO,
                current.getName() + " transferiu a propriedade do projeto \"" + project.getName()
                        + "\" para " + newOwner.getUser().getName(),
                current, projectId, null);
        return toResponse(project);
    }

    // ---------- auxiliares ----------

    private ProjectResponse toResponse(Project project) {
        long totalTasks = taskRepository.countByProjectId(project.getId());
        long completedTasks = taskRepository.countByProjectIdAndStatus(project.getId(), TaskStatus.CONCLUIDA);
        long members = projectMemberRepository.countByProjectId(project.getId());
        return projectMapper.toResponse(project, members, totalTasks, completedTasks);
    }

    /** O prazo só é validado quando muda (assim dá para editar um projeto antigo sem mexer no prazo). */
    private void validateDeadline(LocalDate newDeadline, LocalDate currentDeadline) {
        if (newDeadline == null || newDeadline.equals(currentDeadline)) {
            return;
        }
        if (newDeadline.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("O prazo do projeto não pode estar no passado");
        }
    }
}
