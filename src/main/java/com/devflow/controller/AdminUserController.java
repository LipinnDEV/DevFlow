package com.devflow.controller;

import com.devflow.dto.common.PageResponse;
import com.devflow.dto.user.UserResponse;
import com.devflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints exclusivos de administrador. A proteção é dupla (defesa em profundidade):
 * a regra de URL no SecurityConfig e o @PreAuthorize aqui.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administração", description = "Gestão de usuários (somente ADMIN)")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Lista usuários, com busca por nome ou e-mail")
    public PageResponse<UserResponse> list(
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return userService.listUsers(search, pageable);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Reativa a conta de um usuário")
    public UserResponse activate(@PathVariable Long id) {
        return userService.changeActiveStatus(id, true);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Desativa a conta de um usuário")
    public UserResponse deactivate(@PathVariable Long id) {
        return userService.changeActiveStatus(id, false);
    }
}
