package com.devflow.service;

import com.devflow.dto.common.PageResponse;
import com.devflow.dto.user.ChangePasswordRequest;
import com.devflow.dto.user.UpdateUserRequest;
import com.devflow.dto.user.UserProfileResponse;
import com.devflow.dto.user.UserResponse;
import com.devflow.entity.User;
import com.devflow.enums.ActivityType;
import com.devflow.enums.Role;
import com.devflow.enums.TaskStatus;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.ForbiddenOperationException;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.mapper.UserMapper;
import com.devflow.repository.ProjectMemberRepository;
import com.devflow.repository.TaskRepository;
import com.devflow.repository.UserRepository;
import com.devflow.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final ActivityService activityService;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {
        User user = currentUserService.getCurrentUser();
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                projectMemberRepository.countByUserId(user.getId()),
                taskRepository.countByAssigneeId(user.getId()),
                taskRepository.countByAssigneeIdAndStatus(user.getId(), TaskStatus.CONCLUIDA));
    }

    /** Usuário comum só consulta a si mesmo; ADMIN consulta qualquer um. */
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User current = currentUserService.getCurrentUser();
        if (!ProjectAccessService.isAdmin(current) && !current.getId().equals(id)) {
            throw new ForbiddenOperationException("Você só pode consultar o seu próprio usuário");
        }
        return userMapper.toResponse(findUser(id));
    }

    public UserResponse updateMyProfile(UpdateUserRequest request) {
        User user = currentUserService.getCurrentUser();
        user.setName(request.name().trim());
        return userMapper.toResponse(user);
    }

    public void changePassword(ChangePasswordRequest request) {
        User user = currentUserService.getCurrentUser();

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessRuleException("A senha atual está incorreta");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessRuleException("A nova senha deve ser diferente da senha atual");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        log.info("Senha alterada pelo usuário {}", user.getEmail());
    }

    /** Exclusão lógica: a conta fica inativa e não consegue mais fazer login. */
    public void deactivateMyAccount() {
        User user = currentUserService.getCurrentUser();

        if (user.getRole() == Role.ADMIN) {
            throw new BusinessRuleException("Administradores não podem desativar a própria conta");
        }

        user.setActive(false);
        activityService.register(ActivityType.USUARIO_DESATIVADO,
                user.getName() + " desativou a própria conta", user, null, null);
        log.info("Conta desativada: {}", user.getEmail());
    }

    // ---------- operações de administrador ----------

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(String search, Pageable pageable) {
        Page<User> page;
        if (search == null || search.isBlank()) {
            page = userRepository.findAll(pageable);
        } else {
            String term = search.trim();
            page = userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term, pageable);
        }
        return PageResponse.from(page.map(userMapper::toResponse));
    }

    public UserResponse changeActiveStatus(Long userId, boolean active) {
        User admin = currentUserService.getCurrentUser();
        User target = findUser(userId);

        if (admin.getId().equals(target.getId())) {
            throw new BusinessRuleException("Você não pode alterar o status da sua própria conta");
        }

        if (target.isActive() != active) {
            target.setActive(active);
            ActivityType type = active ? ActivityType.USUARIO_ATIVADO : ActivityType.USUARIO_DESATIVADO;
            String action = active ? " ativou" : " desativou";
            activityService.register(type, admin.getName() + action + " a conta de " + target.getName(),
                    admin, null, null);
        }
        return userMapper.toResponse(target);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }
}
