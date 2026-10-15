package com.devflow.controller;

import com.devflow.dto.activity.ActivityResponse;
import com.devflow.dto.common.PageResponse;
import com.devflow.dto.project.AddMemberRequest;
import com.devflow.dto.project.MemberResponse;
import com.devflow.dto.project.ProjectRequest;
import com.devflow.dto.project.ProjectResponse;
import com.devflow.dto.project.TransferOwnershipRequest;
import com.devflow.enums.ProjectStatus;
import com.devflow.service.ActivityService;
import com.devflow.service.ProjectService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Projetos", description = "Projetos, membros e histórico")
public class ProjectController {

    private final ProjectService projectService;
    private final ActivityService activityService;

    @GetMapping
    @Operation(summary = "Lista os projetos do usuário, com filtros, ordenação e paginação")
    public PageResponse<ProjectResponse> list(
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) String name,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return projectService.list(status, name, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um projeto (o criador vira proprietário)")
    public ProjectResponse create(@Valid @RequestBody ProjectRequest request) {
        return projectService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um projeto (inclui o percentual de conclusão)")
    public ProjectResponse getById(@PathVariable Long id) {
        return projectService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita um projeto (membros e proprietário)")
    public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return projectService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um projeto (apenas o proprietário ou um administrador)")
    public void delete(@PathVariable Long id) {
        projectService.delete(id);
    }

    // ---------- membros ----------

    @GetMapping("/{id}/members")
    @Operation(summary = "Lista os membros do projeto")
    public List<MemberResponse> listMembers(@PathVariable Long id) {
        return projectService.listMembers(id);
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona um membro pelo e-mail (apenas o proprietário)")
    public MemberResponse addMember(@PathVariable Long id, @Valid @RequestBody AddMemberRequest request) {
        return projectService.addMember(id, request);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um membro do projeto (apenas o proprietário)")
    public void removeMember(@PathVariable Long id, @PathVariable Long userId) {
        projectService.removeMember(id, userId);
    }

    @PutMapping("/{id}/owner")
    @Operation(summary = "Transfere a propriedade do projeto para outro membro")
    public ProjectResponse transferOwnership(@PathVariable Long id, @Valid @RequestBody TransferOwnershipRequest request) {
        return projectService.transferOwnership(id, request);
    }

    // ---------- histórico ----------

    @GetMapping("/{id}/activities")
    @Operation(summary = "Histórico de atividades do projeto")
    public PageResponse<ActivityResponse> activities(
            @PathVariable Long id,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return activityService.listByProject(id, pageable);
    }
}
