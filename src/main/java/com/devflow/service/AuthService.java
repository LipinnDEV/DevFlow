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
import com.devflow.security.CustomUserDetails;
import com.devflow.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final ActivityService activityService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Já existe um usuário cadastrado com este e-mail");
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password())) // só o hash vai para o banco
                .role(Role.USER) // quem se cadastra pela API nunca vira ADMIN
                .active(true)
                .build();

        User saved = userRepository.save(user);
        activityService.register(ActivityType.USUARIO_REGISTRADO,
                saved.getName() + " criou sua conta", saved, null, null);
        log.info("Novo usuário cadastrado: {}", email);
        return userMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        // A mesma mensagem para "e-mail inexistente" e "senha errada": não revela quais e-mails existem
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Tentativa de login com senha incorreta: {}", email);
            throw new InvalidCredentialsException();
        }

        if (!user.isActive()) {
            throw new AccountDisabledException();
        }

        String token = jwtService.generateToken(new CustomUserDetails(user));
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs(), userMapper.toResponse(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
