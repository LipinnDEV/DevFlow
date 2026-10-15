package com.devflow.service;

import com.devflow.entity.Project;
import com.devflow.entity.User;
import com.devflow.enums.Role;
import com.devflow.exception.ForbiddenOperationException;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.repository.ProjectMemberRepository;
import com.devflow.repository.ProjectRepository;
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
import static org.mockito.Mockito.when;

/** Testa as regras de permissão sobre projetos. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectAccessServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;

    private ProjectAccessService accessService;

    private User owner;
    private User member;
    private User outsider;
    private User admin;
    private Project project;

    @BeforeEach
    void setUp() {
        accessService = new ProjectAccessService(projectRepository, projectMemberRepository);
        owner = TestFactory.user(1L, "Ana", Role.USER);
        member = TestFactory.user(2L, "Bruno", Role.USER);
        outsider = TestFactory.user(3L, "Carla", Role.USER);
        admin = TestFactory.user(4L, "Admin", Role.ADMIN);
        project = TestFactory.project(10L, owner, null);

        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndUserId(10L, 1L)).thenReturn(true);
        when(projectMemberRepository.existsByProjectIdAndUserId(10L, 2L)).thenReturn(true);
        when(projectMemberRepository.existsByProjectIdAndUserId(10L, 3L)).thenReturn(false);
    }

    @Test
    @DisplayName("Membro do projeto pode acessá-lo")
    void membroPodeAcessar() {
        assertThat(accessService.getProjectForMember(10L, member)).isSameAs(project);
        assertThat(accessService.getProjectForMember(10L, owner)).isSameAs(project);
    }

    @Test
    @DisplayName("Quem não participa do projeto não pode acessá-lo")
    void naoMembroNaoPodeAcessar() {
        assertThatThrownBy(() -> accessService.getProjectForMember(10L, outsider))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("Administrador acessa qualquer projeto, mesmo sem ser membro")
    void adminAcessaQualquerProjeto() {
        assertThat(accessService.getProjectForMember(10L, admin)).isSameAs(project);
    }

    @Test
    @DisplayName("Apenas o proprietário (ou admin) pode gerenciar/excluir o projeto")
    void apenasProprietarioGerencia() {
        assertThat(accessService.getProjectForOwner(10L, owner)).isSameAs(project);
        assertThat(accessService.getProjectForOwner(10L, admin)).isSameAs(project);

        assertThatThrownBy(() -> accessService.getProjectForOwner(10L, member))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> accessService.getProjectForOwner(10L, outsider))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("Projeto inexistente deve lançar ResourceNotFoundException")
    void projetoInexistente() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessService.getProjectForMember(99L, owner))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
