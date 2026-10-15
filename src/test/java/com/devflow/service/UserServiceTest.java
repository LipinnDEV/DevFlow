package com.devflow.service;

import com.devflow.dto.user.ChangePasswordRequest;
import com.devflow.dto.user.UpdateUserRequest;
import com.devflow.dto.user.UserResponse;
import com.devflow.entity.User;
import com.devflow.enums.ActivityType;
import com.devflow.enums.Role;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.ForbiddenOperationException;
import com.devflow.mapper.UserMapper;
import com.devflow.repository.ProjectMemberRepository;
import com.devflow.repository.TaskRepository;
import com.devflow.repository.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private ActivityService activityService;

    private UserService userService;
    private User maria;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, projectMemberRepository, taskRepository,
                passwordEncoder, currentUserService, activityService, new UserMapper());
        maria = TestFactory.user(1L, "Maria", Role.USER);
        when(currentUserService.getCurrentUser()).thenReturn(maria);
    }

    @Test
    @DisplayName("Alterar senha: deve gravar o hash da nova senha")
    void changePassword_sucesso() {
        when(passwordEncoder.matches("SenhaAtual1", "hash")).thenReturn(true);
        when(passwordEncoder.matches("NovaSenha1", "hash")).thenReturn(false);
        when(passwordEncoder.encode("NovaSenha1")).thenReturn("novo-hash");

        userService.changePassword(new ChangePasswordRequest("SenhaAtual1", "NovaSenha1"));

        assertThat(maria.getPassword()).isEqualTo("novo-hash");
    }

    @Test
    @DisplayName("Alterar senha: senha atual incorreta deve lançar BusinessRuleException")
    void changePassword_senhaAtualIncorreta() {
        when(passwordEncoder.matches("errada123", "hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(new ChangePasswordRequest("errada123", "NovaSenha1")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("senha atual");

        assertThat(maria.getPassword()).isEqualTo("hash");
    }

    @Test
    @DisplayName("Alterar senha: nova senha igual à atual deve ser rejeitada")
    void changePassword_novaIgualAAtual() {
        when(passwordEncoder.matches("SenhaAtual1", "hash")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(new ChangePasswordRequest("SenhaAtual1", "SenhaAtual1")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Atualizar perfil: deve alterar o nome")
    void updateMyProfile_alteraNome() {
        UserResponse response = userService.updateMyProfile(new UpdateUserRequest("  Maria Souza  "));

        assertThat(response.name()).isEqualTo("Maria Souza");
        assertThat(maria.getName()).isEqualTo("Maria Souza");
    }

    @Test
    @DisplayName("Desativar conta: usuário comum fica inativo e a ação é registrada")
    void deactivateMyAccount_usuarioComum() {
        userService.deactivateMyAccount();

        assertThat(maria.isActive()).isFalse();
        verify(activityService).register(eq(ActivityType.USUARIO_DESATIVADO), anyString(), eq(maria), isNull(), isNull());
    }

    @Test
    @DisplayName("Desativar conta: administrador não pode desativar a própria conta")
    void deactivateMyAccount_admin() {
        User admin = TestFactory.user(9L, "Admin", Role.ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        assertThatThrownBy(() -> userService.deactivateMyAccount())
                .isInstanceOf(BusinessRuleException.class);

        assertThat(admin.isActive()).isTrue();
    }

    @Test
    @DisplayName("Consultar usuário: usuário comum não pode consultar outro usuário")
    void getById_usuarioComumOutroUsuario() {
        assertThatThrownBy(() -> userService.getById(2L))
                .isInstanceOf(ForbiddenOperationException.class);

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Consultar usuário: administrador pode consultar qualquer usuário")
    void getById_admin() {
        User admin = TestFactory.user(9L, "Admin", Role.ADMIN);
        User bruno = TestFactory.user(2L, "Bruno", Role.USER);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(bruno));

        assertThat(userService.getById(2L).email()).isEqualTo("bruno@test.com");
    }

    @Test
    @DisplayName("Admin: desativa outro usuário e registra a atividade")
    void changeActiveStatus_adminDesativaOutro() {
        User admin = TestFactory.user(9L, "Admin", Role.ADMIN);
        User bruno = TestFactory.user(2L, "Bruno", Role.USER);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(bruno));

        UserResponse response = userService.changeActiveStatus(2L, false);

        assertThat(response.active()).isFalse();
        verify(activityService).register(eq(ActivityType.USUARIO_DESATIVADO), anyString(), eq(admin), isNull(), isNull());
    }

    @Test
    @DisplayName("Admin: não pode alterar o status da própria conta")
    void changeActiveStatus_adminNaPropriaConta() {
        User admin = TestFactory.user(9L, "Admin", Role.ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.changeActiveStatus(9L, false))
                .isInstanceOf(BusinessRuleException.class);
    }
}
