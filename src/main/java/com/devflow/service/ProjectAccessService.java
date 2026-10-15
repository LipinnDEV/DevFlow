package com.devflow.service;

import com.devflow.entity.Project;
import com.devflow.entity.User;
import com.devflow.enums.Role;
import com.devflow.exception.ForbiddenOperationException;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.repository.ProjectMemberRepository;
import com.devflow.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Centraliza as regras de permissão sobre projetos, para que todos os services usem
 * exatamente a mesma lógica:
 * - membro (ou dono) do projeto: pode ver e editar;
 * - dono do projeto: pode gerenciar membros e excluir;
 * - ADMIN: pode tudo.
 */
@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public static boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    public Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado"));
    }

    /** Retorna o projeto se o usuário participa dele (ou é ADMIN). */
    public Project getProjectForMember(Long projectId, User user) {
        Project project = findProject(projectId);
        if (!isAdmin(user) && !projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new ForbiddenOperationException("Você não participa deste projeto");
        }
        return project;
    }

    /** Retorna o projeto se o usuário é o proprietário (ou ADMIN). */
    public Project getProjectForOwner(Long projectId, User user) {
        Project project = findProject(projectId);
        if (!isAdmin(user) && !project.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenOperationException("Apenas o proprietário do projeto pode realizar esta ação");
        }
        return project;
    }
}
