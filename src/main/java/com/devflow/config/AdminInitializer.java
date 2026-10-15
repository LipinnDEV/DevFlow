package com.devflow.config;

import com.devflow.entity.User;
import com.devflow.enums.Role;
import com.devflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria o administrador inicial na primeira execução, usando as variáveis de ambiente
 * ADMIN_EMAIL e ADMIN_PASSWORD (nada de senha fixa no código).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.name:Administrador}")
    private String adminName;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("ADMIN_EMAIL/ADMIN_PASSWORD não informados: nenhum administrador inicial foi criado");
            return;
        }

        String email = adminEmail.trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User admin = User.builder()
                .name(adminName)
                .email(email)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .active(true)
                .build();
        userRepository.save(admin);
        log.info("Administrador inicial criado: {}", email);
    }
}
