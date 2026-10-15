package com.devflow.service;

import com.devflow.dto.comment.CommentRequest;
import com.devflow.dto.comment.CommentResponse;
import com.devflow.entity.Comment;
import com.devflow.entity.Project;
import com.devflow.entity.Task;
import com.devflow.entity.User;
import com.devflow.enums.ActivityType;
import com.devflow.enums.Role;
import com.devflow.enums.TaskStatus;
import com.devflow.exception.ForbiddenOperationException;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.mapper.CommentMapper;
import com.devflow.mapper.UserMapper;
import com.devflow.repository.CommentRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProjectAccessService accessService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private ActivityService activityService;

    private CommentService commentService;
    private User ana;
    private Task task;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, taskRepository, accessService,
                currentUserService, activityService, new CommentMapper(new UserMapper()));

        ana = TestFactory.user(1L, "Ana", Role.USER);
        Project project = TestFactory.project(1L, ana, null);
        task = TestFactory.task(100L, project, ana, null, TaskStatus.TODO);

        when(currentUserService.getCurrentUser()).thenReturn(ana);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(accessService.getProjectForMember(1L, ana)).thenReturn(project);
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(5L);
            return c;
        });
    }

    @Test
    @DisplayName("Comentar: salva o comentário com autor e tarefa e registra a atividade")
    void create_sucesso() {
        CommentResponse response = commentService.create(100L, new CommentRequest("  Vou começar hoje  "));

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.text()).isEqualTo("Vou começar hoje");
        assertThat(response.author().id()).isEqualTo(1L);
        assertThat(response.taskId()).isEqualTo(100L);
        verify(activityService).register(eq(ActivityType.COMENTARIO_CRIADO), anyString(), eq(ana), eq(1L), eq(100L));
    }

    @Test
    @DisplayName("Comentar: quem não participa do projeto não pode comentar")
    void create_naoMembro() {
        when(accessService.getProjectForMember(1L, ana)).thenThrow(new ForbiddenOperationException("Você não participa deste projeto"));

        assertThatThrownBy(() -> commentService.create(100L, new CommentRequest("Oi")))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Comentar: tarefa inexistente deve lançar ResourceNotFoundException")
    void create_tarefaInexistente() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(999L, new CommentRequest("Oi")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
