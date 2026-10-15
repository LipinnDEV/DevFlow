package com.devflow.service;

import com.devflow.dto.common.PageResponse;
import com.devflow.dto.task.TaskFilter;
import com.devflow.dto.task.TaskRequest;
import com.devflow.dto.task.TaskResponse;
import com.devflow.dto.task.UpdateTaskStatusRequest;
import com.devflow.entity.Project;
import com.devflow.entity.Tag;
import com.devflow.entity.Task;
import com.devflow.entity.User;
import com.devflow.enums.ActivityType;
import com.devflow.enums.ProjectStatus;
import com.devflow.enums.TaskPriority;
import com.devflow.enums.TaskStatus;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.ForbiddenOperationException;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.mapper.TaskMapper;
import com.devflow.repository.ProjectMemberRepository;
import com.devflow.repository.TagRepository;
import com.devflow.repository.TaskRepository;
import com.devflow.repository.UserRepository;
import com.devflow.security.CurrentUserService;
import com.devflow.specification.TaskSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final ProjectAccessService accessService;
    private final CurrentUserService currentUserService;
    private final ActivityService activityService;
    private final TaskMapper taskMapper;

    // ---------- consultas ----------

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listByProject(Long projectId, TaskFilter filter, Pageable pageable) {
        User current = currentUserService.getCurrentUser();
        accessService.getProjectForMember(projectId, current);

        Specification<Task> spec = TaskSpecifications.inProject(projectId).and(filterSpec(filter));
        return PageResponse.from(taskRepository.findAll(spec, pageable).map(taskMapper::toResponse));
    }

    /** Todas as tarefas dos projetos do usuário (ADMIN vê todas). Útil com ?overdue=true ou ?assigneeId=. */
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listAll(TaskFilter filter, Pageable pageable) {
        User current = currentUserService.getCurrentUser();

        Specification<Task> spec = filterSpec(filter);
        if (!ProjectAccessService.isAdmin(current)) {
            spec = spec.and(TaskSpecifications.inProjectsOf(current.getId()));
        }
        return PageResponse.from(taskRepository.findAll(spec, pageable).map(taskMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(Long taskId) {
        User current = currentUserService.getCurrentUser();
        Task task = findTask(taskId);
        accessService.getProjectForMember(task.getProject().getId(), current);
        return taskMapper.toResponse(task);
    }

    // ---------- comandos ----------

    public TaskResponse create(Long projectId, TaskRequest request) {
        User current = currentUserService.getCurrentUser();
        Project project = accessService.getProjectForMember(projectId, current);

        validateDeadline(request.deadline(), null, project);
        User assignee = resolveAssignee(project, request.assigneeId());

        Task task = Task.builder()
                .title(request.title().trim())
                .description(request.description())
                .priority(request.priority() != null ? request.priority() : TaskPriority.MEDIA)
                .assignee(assignee)
                .project(project)
                .createdBy(current)
                .deadline(request.deadline())
                .tags(resolveTags(request.tags()))
                .build();

        if (request.status() != null) {
            applyStatus(task, request.status());
        }

        Task saved = taskRepository.save(task);
        activityService.register(ActivityType.TAREFA_CRIADA,
                current.getName() + " criou a tarefa \"" + saved.getTitle() + "\"",
                current, projectId, saved.getId());
        syncProjectStatus(project, current);
        log.info("Tarefa {} criada no projeto {}", saved.getId(), projectId);
        return taskMapper.toResponse(saved);
    }

    public TaskResponse update(Long taskId, TaskRequest request) {
        User current = currentUserService.getCurrentUser();
        Task task = findTask(taskId);
        Project project = accessService.getProjectForMember(task.getProject().getId(), current);

        validateDeadline(request.deadline(), task.getDeadline(), project);
        TaskStatus oldStatus = task.getStatus();

        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        task.setDeadline(request.deadline());
        task.setAssignee(resolveAssignee(project, request.assigneeId()));
        task.getTags().clear();
        task.getTags().addAll(resolveTags(request.tags()));

        if (request.status() != null) {
            applyStatus(task, request.status());
        } else if (task.getStatus() == TaskStatus.CONCLUIDA && task.getAssignee() == null) {
            throw new BusinessRuleException("Não é possível remover o responsável de uma tarefa concluída");
        }

        activityService.register(ActivityType.TAREFA_ATUALIZADA,
                current.getName() + " atualizou a tarefa \"" + task.getTitle() + "\"",
                current, project.getId(), task.getId());
        registerStatusChange(task, oldStatus, current);
        syncProjectStatus(project, current);
        return taskMapper.toResponse(task);
    }

    public TaskResponse updateStatus(Long taskId, UpdateTaskStatusRequest request) {
        User current = currentUserService.getCurrentUser();
        Task task = findTask(taskId);
        Project project = accessService.getProjectForMember(task.getProject().getId(), current);

        TaskStatus oldStatus = task.getStatus();
        applyStatus(task, request.status());

        registerStatusChange(task, oldStatus, current);
        syncProjectStatus(project, current);
        return taskMapper.toResponse(task);
    }

    public void delete(Long taskId) {
        User current = currentUserService.getCurrentUser();
        Task task = findTask(taskId);
        Project project = accessService.getProjectForMember(task.getProject().getId(), current);

        boolean isProjectOwner = project.getOwner().getId().equals(current.getId());
        boolean isTaskCreator = task.getCreatedBy().getId().equals(current.getId());
        if (!ProjectAccessService.isAdmin(current) && !isProjectOwner && !isTaskCreator) {
            throw new ForbiddenOperationException(
                    "Apenas o proprietário do projeto ou o criador da tarefa podem excluí-la");
        }

        taskRepository.delete(task);
        activityService.register(ActivityType.TAREFA_EXCLUIDA,
                current.getName() + " excluiu a tarefa \"" + task.getTitle() + "\"",
                current, project.getId(), taskId);
        syncProjectStatus(project, current);
    }

    // ---------- regras de negócio ----------

    /**
     * Aplica um novo status à tarefa respeitando as regras:
     * - não pode ser concluída sem responsável;
     * - ao concluir, registra a data de conclusão; ao reabrir, limpa essa data.
     */
    private void applyStatus(Task task, TaskStatus newStatus) {
        if (newStatus == TaskStatus.CONCLUIDA) {
            if (task.getAssignee() == null) {
                throw new BusinessRuleException("Uma tarefa não pode ser concluída sem responsável");
            }
            if (task.getStatus() != TaskStatus.CONCLUIDA) {
                task.setCompletedAt(LocalDateTime.now());
            }
        } else {
            task.setCompletedAt(null);
        }
        task.setStatus(newStatus);
    }

    private void registerStatusChange(Task task, TaskStatus oldStatus, User actor) {
        if (oldStatus == task.getStatus()) {
            return;
        }
        if (task.getStatus() == TaskStatus.CONCLUIDA) {
            activityService.register(ActivityType.TAREFA_CONCLUIDA,
                    actor.getName() + " concluiu a tarefa \"" + task.getTitle() + "\"",
                    actor, task.getProject().getId(), task.getId());
        } else {
            activityService.register(ActivityType.STATUS_ALTERADO,
                    actor.getName() + " alterou o status da tarefa \"" + task.getTitle()
                            + "\" de " + oldStatus + " para " + task.getStatus(),
                    actor, task.getProject().getId(), task.getId());
        }
    }

    /**
     * Mantém o status do projeto coerente com as tarefas:
     * todas concluídas -> projeto CONCLUIDO; voltou a ter tarefa aberta -> volta para EM_ANDAMENTO.
     */
    private void syncProjectStatus(Project project, User actor) {
        long total = taskRepository.countByProjectId(project.getId());
        long completed = taskRepository.countByProjectIdAndStatus(project.getId(), TaskStatus.CONCLUIDA);

        if (total > 0 && total == completed && project.getStatus() != ProjectStatus.CONCLUIDO) {
            project.setStatus(ProjectStatus.CONCLUIDO);
            activityService.register(ActivityType.PROJETO_CONCLUIDO,
                    "O projeto \"" + project.getName() + "\" foi concluído: todas as tarefas foram finalizadas",
                    actor, project.getId(), null);
        } else if (completed < total && project.getStatus() == ProjectStatus.CONCLUIDO) {
            project.setStatus(ProjectStatus.EM_ANDAMENTO);
        }
    }

    /** O prazo só é validado quando muda. Nunca pode ser no passado nem depois do prazo do projeto. */
    private void validateDeadline(LocalDate newDeadline, LocalDate currentDeadline, Project project) {
        if (newDeadline == null || newDeadline.equals(currentDeadline)) {
            return;
        }
        if (newDeadline.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("O prazo da tarefa não pode estar no passado");
        }
        if (project.getDeadline() != null && newDeadline.isAfter(project.getDeadline())) {
            throw new BusinessRuleException("O prazo da tarefa não pode ser posterior ao prazo do projeto");
        }
    }

    /** O responsável precisa existir, estar ativo e ser membro do projeto. */
    private User resolveAssignee(Project project, Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        User user = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("Responsável não encontrado"));
        if (!user.isActive()) {
            throw new BusinessRuleException("O responsável informado está com a conta desativada");
        }
        if (!projectMemberRepository.existsByProjectIdAndUserId(project.getId(), assigneeId)) {
            throw new BusinessRuleException("O responsável precisa ser membro do projeto");
        }
        return user;
    }

    /** Reaproveita tags existentes (pelo nome) e cria as que ainda não existem. */
    private Set<Tag> resolveTags(Set<String> names) {
        if (names == null) {
            return new HashSet<>();
        }
        return names.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(name -> !name.isEmpty())
                .distinct()
                .map(name -> tagRepository.findByName(name)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build())))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Specification<Task> filterSpec(TaskFilter filter) {
        TaskFilter f = filter != null ? filter : new TaskFilter(null, null, null, null, null, null);
        return Specification.where(TaskSpecifications.hasStatus(f.status()))
                .and(TaskSpecifications.hasPriority(f.priority()))
                .and(TaskSpecifications.hasAssignee(f.assigneeId()))
                .and(TaskSpecifications.hasTag(f.tag()))
                .and(TaskSpecifications.isOverdue(f.overdue()))
                .and(TaskSpecifications.titleContains(f.search()));
    }

    private Task findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada"));
    }
}
