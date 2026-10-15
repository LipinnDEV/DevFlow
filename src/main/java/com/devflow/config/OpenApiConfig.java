package com.devflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI devFlowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DevFlow API")
                        .version("1.0.0")
                        .description("""
                                API REST da plataforma DevFlow — gerenciamento de projetos, tarefas e equipes.

                                Como testar: faça login em **POST /api/auth/login**, copie o token retornado, \
                                clique em **Authorize** (cadeado) e cole o token.""")
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
