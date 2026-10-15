package com.devflow.security;

import com.devflow.entity.User;
import com.devflow.exception.InvalidCredentialsException;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Devolve o usuário autenticado na requisição atual (a partir do token JWT). */
@Component
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            throw new InvalidCredentialsException("Usuário não autenticado");
        }
        return userRepository.findById(details.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado"));
    }
}
