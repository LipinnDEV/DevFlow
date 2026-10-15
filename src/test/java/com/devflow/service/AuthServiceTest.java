package com.devflow.service;

import com.devflow.dto.auth.AuthResponse;
import com.devflow.dto.auth.LoginRequest;
import com.devflow.dto.auth.RegisterRequest;
import com.devflow.dto.user.UserResponse;
import com.devflow.entity.User;
import com.devflow.enums.ActivityType;
import com.devflow.enums.Role;
import com.devflow.exception.AccountDisabledException;
import com.devflow.exception.ConflictException;
import com.devflow.exception.InvalidCredentialsException;
import com.devflow.mapper.UserMapper;
import com.devflow.repository.UserRepository;
import com.devflow.security.JwtService;
import com.devflow.support.TestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private ActivityService activityService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, new UserMapper(), activityService);
    }

    // ---------------- cadastro ----------------

    @Test
    @DisplayName("Cadastro: deve criptografar a senha, definir papel USER e registrar atividade")
    void register_deveCriptografarSenhaESalvarUsuario() {
        when(userRepository.existsByEmail("maria@email.com")).thenReturn(false);
        when(passwordEncoder.encode("Senha1234")).thenReturn("hash-bcrypt");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse response = authService.register(new RegisterRequest("Maria", "Maria@Email.com", "Senha1234"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getPassword()).isEqualTo("hash-bcrypt").isNotEqualTo("Senha1234");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.isActive()).isTrue();
        assertThat(response.email()).isEqualTo("maria@email.com");
        verify(activityService).register(eq(ActivityType.USUARIO_REGISTRADO), anyString(), any(User.class), isNull(), isNull());
    }

    @Test
    @DisplayName("Cadastro: e-mail já existente deve lançar ConflictException")
    void register_emailDuplicado() {
        when(userRepository.existsByEmail("maria@email.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Maria", "maria@email.com", "Senha1234")))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
    }

    // ---------------- login ----------------

    @Test
    @DisplayName("Login: credenciais corretas devem retornar o token JWT")
    void login_sucesso() {
        User user = TestFactory.user(1L, "Maria", Role.USER);
        when(userRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Senha1234", "hash")).thenReturn(true);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(3_600_000L);

        AuthResponse response = authService.login(new LoginRequest("Maria@email.com", "Senha1234"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("maria@test.com");
    }

    @Test
    @DisplayName("Login: senha incorreta deve lançar InvalidCredentialsException")
    void login_senhaIncorreta() {
        User user = TestFactory.user(1L, "Maria", Role.USER);
        when(userRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada123", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("maria@email.com", "errada123")))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Login: e-mail inexistente deve lançar InvalidCredentialsException")
    void login_emailInexistente() {
        when(userRepository.findByEmail("ninguem@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ninguem@email.com", "Senha1234")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Login: usuário desativado não pode fazer login")
    void login_usuarioDesativado() {
        User user = TestFactory.user(1L, "Maria", Role.USER);
        user.setActive(false);
        when(userRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Senha1234", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("maria@email.com", "Senha1234")))
                .isInstanceOf(AccountDisabledException.class);

        verifyNoInteractions(jwtService);
    }
}
