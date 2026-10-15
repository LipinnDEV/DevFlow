package com.devflow.controller;

import com.devflow.dto.common.PageResponse;
import com.devflow.dto.task.TaskFilter;
import com.devflow.dto.task.TaskRequest;
import com.devflow.dto.task.TaskResponse;
import com.devflow.dto.task.UpdateTaskStatusRequest;
import com.devflow.service.TaskService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tarefas", description = "Tarefas dos projetos")
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/projects/{projectId}/tasks")
    @Operation(summary = "Lista as tarefas de um projeto (filtros: status, priority, assigneeId, tag, overdue, search)")
    public PageResponse<TaskResponse> listByProject(
            @PathVariable Long projectId,
            @ParameterObject TaskFilter filter,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return taskService.listByProject(projectId, filter, pageable);
    }

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma tarefa em um projeto")
    public TaskResponse create(@PathVariable Long projectId, @Valid @RequestBody TaskRequest request) {
        return taskService.create(projectId, request);
    }

    @GetMapping("/tasks")
    @Operation(summary = "Lista as tarefas de todos os projetos do usuário (use overdue=true para as atrasadas)")
    public PageResponse<TaskResponse> listAll(
            @ParameterObject TaskFilter filter,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return taskService.listAll(filter, pageable);
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "Detalha uma tarefa")
    public TaskResponse getById(@PathVariable Long id) {
        return taskService.getById(id);
    }

    @PutMapping("/tasks/{id}")
    @Operation(summary = "Atualiza uma tarefa")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return taskService.update(id, request);
    }

    @PatchMapping("/tasks/{id}/status")
    @Operation(summary = "Altera somente o status da tarefa (concluir exige responsável)")
    public TaskResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateTaskStatusRequest request) {
        return taskService.updateStatus(id, request);
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma tarefa (proprietário do projeto, criador da tarefa ou administrador)")
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }
}
