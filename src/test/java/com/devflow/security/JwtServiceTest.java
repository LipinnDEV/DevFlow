package com.devflow.security;

import com.devflow.enums.Role;
import com.devflow.support.TestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123";

    private final JwtService jwtService = new JwtService(SECRET, 60_000);
    private final CustomUserDetails ana = new CustomUserDetails(TestFactory.user(1L, "Ana", Role.USER));

    @Test
    @DisplayName("Deve gerar um token válido contendo o e-mail do usuário")
    void deveGerarTokenValido() {
        String token = jwtService.generateToken(ana);

        assertThat(jwtService.extractUsername(token)).isEqualTo("ana@test.com");
        assertThat(jwtService.isTokenValid(token, ana)).isTrue();
    }

    @Test
    @DisplayName("Token de um usuário não vale para outro")
    void tokenNaoVaiParaOutroUsuario() {
        CustomUserDetails bruno = new CustomUserDetails(TestFactory.user(2L, "Bruno", Role.USER));
        String token = jwtService.generateToken(ana);

        assertThat(jwtService.isTokenValid(token, bruno)).isFalse();
    }

    @Test
    @DisplayName("Token expirado deve ser inválido")
    void tokenExpiradoEInvalido() {
        JwtService expiradoLogoNaCriacao = new JwtService(SECRET, -1_000);
        String token = expiradoLogoNaCriacao.generateToken(ana);

        assertThat(expiradoLogoNaCriacao.isTokenValid(token, ana)).isFalse();
    }

    @Test
    @DisplayName("Token adulterado deve ser inválido")
    void tokenAdulteradoEInvalido() {
        String token = jwtService.generateToken(ana);
        String adulterado = token.substring(0, token.length() - 3) + "abc";

        assertThat(jwtService.isTokenValid(adulterado, ana)).isFalse();
    }

    @Test
    @DisplayName("Token assinado com outro segredo deve ser inválido")
    void tokenDeOutroSegredoEInvalido() {
        JwtService outro = new JwtService("outro-segredo-outro-segredo-outro-segredo-99", 60_000);
        String token = outro.generateToken(ana);

        assertThat(jwtService.isTokenValid(token, ana)).isFalse();
    }

    @Test
    @DisplayName("Segredo curto demais deve impedir a criação do serviço")
    void segredoCurtoDeveFalhar() {
        assertThatThrownBy(() -> new JwtService("curto", 1000))
                .isInstanceOf(IllegalStateException.class);
    }
}
