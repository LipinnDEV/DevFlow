package com.devflow.service;

import com.devflow.dto.comment.CommentRequest;
import com.devflow.dto.comment.CommentResponse;
import com.devflow.dto.common.PageResponse;
import com.devflow.entity.Comment;
import com.devflow.entity.Task;
import com.devflow.entity.User;
import com.devflow.enums.ActivityType;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.mapper.CommentMapper;
import com.devflow.repository.CommentRepository;
import com.devflow.repository.TaskRepository;
import com.devflow.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final ProjectAccessService accessService;
    private final CurrentUserService currentUserService;
    private final ActivityService activityService;
    private final CommentMapper commentMapper;

    public CommentResponse create(Long taskId, CommentRequest request) {
        User current = currentUserService.getCurrentUser();
        Task task = findTask(taskId);
        // Só quem participa do projeto da tarefa pode comentar
        accessService.getProjectForMember(task.getProject().getId(), current);

        Comment comment = Comment.builder()
                .text(request.text().trim())
                .author(current)
                .task(task)
                .build();
        Comment saved = commentRepository.save(comment);

        activityService.register(ActivityType.COMENTARIO_CRIADO,
                current.getName() + " comentou na tarefa \"" + task.getTitle() + "\"",
                current, task.getProject().getId(), task.getId());
        return commentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> listByTask(Long taskId, Pageable pageable) {
        User current = currentUserService.getCurrentUser();
        Task task = findTask(taskId);
        accessService.getProjectForMember(task.getProject().getId(), current);

        return PageResponse.from(commentRepository.findByTaskId(taskId, pageable).map(commentMapper::toResponse));
    }

    private Task findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada"));
    }
}
