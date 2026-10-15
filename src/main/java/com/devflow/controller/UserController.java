package com.devflow.controller;

import com.devflow.dto.activity.ActivityResponse;
import com.devflow.dto.common.PageResponse;
import com.devflow.dto.user.ChangePasswordRequest;
import com.devflow.dto.user.UpdateUserRequest;
import com.devflow.dto.user.UserProfileResponse;
import com.devflow.dto.user.UserResponse;
import com.devflow.service.ActivityService;
import com.devflow.service.UserService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuários", description = "Perfil, senha e histórico do usuário autenticado")
public class UserController {

    private final UserService userService;
    private final ActivityService activityService;

    @GetMapping("/me")
    @Operation(summary = "Perfil do usuário autenticado (com estatísticas)")
    public UserProfileResponse myProfile() {
        return userService.getMyProfile();
    }

    @PutMapping("/me")
    @Operation(summary = "Atualiza os dados do usuário autenticado")
    public UserResponse updateMyProfile(@Valid @RequestBody UpdateUserRequest request) {
        return userService.updateMyProfile(request);
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Altera a senha do usuário autenticado")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa a conta do usuário autenticado (exclusão lógica)")
    public void deactivateMyAccount() {
        userService.deactivateMyAccount();
    }

    @GetMapping("/me/activities")
    @Operation(summary = "Histórico de atividades do usuário autenticado")
    public PageResponse<ActivityResponse> myActivities(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return activityService.listMine(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um usuário (o próprio usuário ou um administrador)")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }
}
