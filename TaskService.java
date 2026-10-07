package com.devflow.service;

import com.devflow.dto.request.CommentRequest;
import com.devflow.dto.request.TaskRequest;
import com.devflow.dto.response.CommentResponse;
import com.devflow.dto.response.TaskResponse;
import com.devflow.entity.Comment;
import com.devflow.entity.Project;
import com.devflow.entity.Task;
import com.devflow.entity.User;
import com.devflow.entity.enums.TaskStatus;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.mapper.EntityMapper;
import com.devflow.repository.CommentRepository;
import com.devflow.repository.TaskRepository;
import com.devflow.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ActivityLogService activityLogService;
    private final EntityMapper mapper;

    @Transactional
    public TaskResponse createTask(
            Long projectId,
            TaskRequest request,
            User currentUser
    ) {
        Project project = projectService.findEntityByIdAndOwner(
                projectId,
                currentUser.getId()
        );

        User assignee = null;

        if (request.assigneeId() != null) {
            assignee = userRepository.findById(request.assigneeId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Usuário responsável não encontrado."
                            )
                    );
        }

        Task task = Task.builder()
                .title(request.title())
                .description(request.description())
                .status(request.status())
                .priority(request.priority())
                .project(project)
                .assignee(assignee)
                .dueDate(request.dueDate())
                .build();

        Task saved = taskRepository.save(task);

        activityLogService.logActivity(
                "CRIAÇÃO_TAREFA",
                "Tarefa '" + saved.getTitle() + "' criada",
                project,
                currentUser
        );

        return mapper.toTaskResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProject(
            Long projectId,
            User currentUser
    ) {
        projectService.findEntityByIdAndOwner(
                projectId,
                currentUser.getId()
        );

        return taskRepository.findByProjectId(projectId)
                .stream()
                .map(mapper::toTaskResponse)
                .toList();
    }

    @Transactional
    public TaskResponse updateTaskStatus(
            Long taskId,
            TaskStatus status,
            User currentUser
    ) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Tarefa não encontrada."
                        )
                );

        projectService.findEntityByIdAndOwner(
                task.getProject().getId(),
                currentUser.getId()
        );

        task.setStatus(status);

        Task updated = taskRepository.save(task);

        activityLogService.logActivity(
                "STATUS_TAREFA",
                "Status da tarefa '" + task.getTitle()
                        + "' alterado para " + status,
                task.getProject(),
                currentUser
        );

        return mapper.toTaskResponse(updated);
    }

    @Transactional
    public CommentResponse addComment(
            Long taskId,
            CommentRequest request,
            User currentUser
    ) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Tarefa não encontrada com ID: " + taskId
                        )
                );

        projectService.findEntityByIdAndOwner(
                task.getProject().getId(),
                currentUser.getId()
        );

        Comment comment = Comment.builder()
                .content(request.content())
                .task(task)
                .author(currentUser)
                .build();

        Comment saved = commentRepository.save(comment);

        return mapper.toCommentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getTaskComments(
            Long taskId,
            User currentUser
    ) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Tarefa não encontrada."
                        )
                );

        projectService.findEntityByIdAndOwner(
                task.getProject().getId(),
                currentUser.getId()
        );

        return commentRepository.findByTaskIdOrderByCreatedAtDesc(taskId)
                .stream()
                .map(mapper::toCommentResponse)
                .toList();
    }
}
