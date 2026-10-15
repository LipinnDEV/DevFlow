package com.devflow.controller;

import com.devflow.dto.comment.CommentRequest;
import com.devflow.dto.comment.CommentResponse;
import com.devflow.dto.common.PageResponse;
import com.devflow.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Comentários", description = "Comentários nas tarefas")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Comenta em uma tarefa")
    public CommentResponse create(@PathVariable Long taskId, @Valid @RequestBody CommentRequest request) {
        return commentService.create(taskId, request);
    }

    @GetMapping
    @Operation(summary = "Lista os comentários de uma tarefa (do mais antigo para o mais recente)")
    public PageResponse<CommentResponse> list(
            @PathVariable Long taskId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return commentService.listByTask(taskId, pageable);
    }
}
