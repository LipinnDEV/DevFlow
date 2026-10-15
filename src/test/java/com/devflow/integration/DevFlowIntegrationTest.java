package com.devflow.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração: sobem a aplicação inteira (com H2 em memória) e chamam a API de verdade,
 * passando por filtro JWT, controllers, services, repositórios e banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DevFlowIntegrationTest {

    private static final String PASSWORD = "Senha1234";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    // ---------------- cenários ----------------

    @Test
    @DisplayName("Fluxo completo: projeto, membro, tarefas, regra de conclusão, comentário, progresso e dashboard")
    void fluxoCompleto() throws Exception {
        String emailAna = uniqueEmail();
        String emailBruno = uniqueEmail();
        String tokenAna = registerAndLogin("Ana", emailAna);
        String tokenBruno = registerAndLogin("Bruno", emailBruno);

        // Ana cria o projeto e adiciona o Bruno
        JsonNode project = call(post("/api/projects"), tokenAna, Map.of(
                "name", "DevFlow",
                "description", "Projeto de teste",
                "deadline", LocalDate.now().plusDays(30).toString()), 201);
        long projectId = project.get("id").asLong();
        assertThat(project.get("status").asText()).isEqualTo("PLANEJADO");
        assertThat(project.get("completionPercentage").asDouble()).isZero();

        JsonNode member = call(post("/api/projects/{id}/members", projectId), tokenAna,
                Map.of("email", emailBruno), 201);
        long brunoId = member.get("userId").asLong();

        // Duas tarefas: uma com responsável (Bruno) e outra sem
        JsonNode task1 = call(post("/api/projects/{id}/tasks", projectId), tokenAna, Map.of(
                "title", "Criar login",
                "priority", "ALTA",
                "assigneeId", brunoId,
                "tags", List.of("Backend")), 201);
        JsonNode task2 = call(post("/api/projects/{id}/tasks", projectId), tokenAna,
                Map.of("title", "Escrever documentação"), 201);
        long taskId1 = task1.get("id").asLong();
        long taskId2 = task2.get("id").asLong();
        assertThat(task1.get("tags").get(0).asText()).isEqualTo("backend");

        // Regra: tarefa sem responsável não pode ser concluída
        call(patch("/api/tasks/{id}/status", taskId2), tokenAna, Map.of("status", "CONCLUIDA"), 422);

        // Bruno comenta e conclui a tarefa dele
        call(post("/api/tasks/{id}/comments", taskId1), tokenBruno, Map.of("text", "Comecei a implementar"), 201);
        JsonNode comments = call(get("/api/tasks/{id}/comments", taskId1), tokenAna, null, 200);
        assertThat(comments.get("totalElements").asInt()).isEqualTo(1);

        JsonNode done = call(patch("/api/tasks/{id}/status", taskId1), tokenBruno, Map.of("status", "CONCLUIDA"), 200);
        assertThat(done.get("completedAt").isNull()).isFalse();

        // Progresso do projeto: 1 de 2 tarefas = 50%
        JsonNode updated = call(get("/api/projects/{id}", projectId), tokenAna, null, 200);
        assertThat(updated.get("completionPercentage").asDouble()).isEqualTo(50.0);
        assertThat(updated.get("totalTasks").asInt()).isEqualTo(2);

        // Filtro e paginação de tarefas
        JsonNode concluded = call(get("/api/projects/{id}/tasks?status=CONCLUIDA", projectId), tokenAna, null, 200);
        assertThat(concluded.get("totalElements").asInt()).isEqualTo(1);

        // Histórico do projeto
        JsonNode activities = call(get("/api/projects/{id}/activities", projectId), tokenAna, null, 200);
        assertThat(activities.get("totalElements").asInt()).isGreaterThanOrEqualTo(5);

        // Dashboard
        JsonNode dashboard = call(get("/api/dashboard"), tokenAna, null, 200);
        assertThat(dashboard.get("totalProjects").asInt()).isEqualTo(1);
        assertThat(dashboard.get("totalTasks").asInt()).isEqualTo(2);
        assertThat(dashboard.get("completedTasks").asInt()).isEqualTo(1);
        assertThat(dashboard.get("tasksByStatus").get("TODO").asInt()).isEqualTo(1);
        assertThat(dashboard.get("userProductivity").size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Rotas protegidas retornam 401 sem token")
    void semTokenRetorna401() throws Exception {
        call(get("/api/projects"), null, null, 401);
        call(get("/api/dashboard"), null, null, 401);
    }

    @Test
    @DisplayName("Token inválido retorna 401")
    void tokenInvalidoRetorna401() throws Exception {
        call(get("/api/projects"), "token.invalido.aqui", null, 401);
    }

    @Test
    @DisplayName("Cadastro com dados inválidos retorna 400 com a lista de erros")
    void cadastroInvalido() throws Exception {
        JsonNode body = call(post("/api/auth/register"), null,
                Map.of("name", "A", "email", "email-invalido", "password", "123"), 400);

        assertThat(body.get("errors").size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Cadastro com e-mail repetido retorna 409")
    void emailRepetido() throws Exception {
        String email = uniqueEmail();
        registerAndLogin("Carla", email);

        call(post("/api/auth/register"), null,
                Map.of("name", "Outra Carla", "email", email, "password", PASSWORD), 409);
    }

    @Test
    @DisplayName("Login com senha errada retorna 401")
    void loginSenhaErrada() throws Exception {
        String email = uniqueEmail();
        registerAndLogin("Diego", email);

        call(post("/api/auth/login"), null, Map.of("email", email, "password", "SenhaErrada9"), 401);
    }

    @Test
    @DisplayName("Usuário que não participa do projeto recebe 403")
    void naoMembroRecebe403() throws Exception {
        String tokenAna = registerAndLogin("Ana", uniqueEmail());
        String tokenIntruso = registerAndLogin("Intruso", uniqueEmail());

        long projectId = createProject(tokenAna);

        call(get("/api/projects/{id}", projectId), tokenIntruso, null, 403);
        call(post("/api/projects/{id}/tasks", projectId), tokenIntruso, Map.of("title", "Invasão"), 403);
        call(delete("/api/projects/{id}", projectId), tokenIntruso, null, 403);
    }

    @Test
    @DisplayName("Membro comum não pode excluir o projeto; o proprietário pode")
    void apenasProprietarioExclui() throws Exception {
        String emailBruno = uniqueEmail();
        String tokenAna = registerAndLogin("Ana", uniqueEmail());
        String tokenBruno = registerAndLogin("Bruno", emailBruno);

        long projectId = createProject(tokenAna);
        call(post("/api/projects/{id}/members", projectId), tokenAna, Map.of("email", emailBruno), 201);

        call(delete("/api/projects/{id}", projectId), tokenBruno, null, 403);
        call(delete("/api/projects/{id}", projectId), tokenAna, null, 204);
        call(get("/api/projects/{id}", projectId), tokenAna, null, 404);
    }

    @Test
    @DisplayName("Adicionar o mesmo membro duas vezes retorna 409")
    void membroDuplicado() throws Exception {
        String emailBruno = uniqueEmail();
        String tokenAna = registerAndLogin("Ana", uniqueEmail());
        registerAndLogin("Bruno", emailBruno);

        long projectId = createProject(tokenAna);
        call(post("/api/projects/{id}/members", projectId), tokenAna, Map.of("email", emailBruno), 201);
        call(post("/api/projects/{id}/members", projectId), tokenAna, Map.of("email", emailBruno), 409);
    }

    @Test
    @DisplayName("Prazo de projeto no passado retorna 422")
    void prazoNoPassado() throws Exception {
        String token = registerAndLogin("Ana", uniqueEmail());

        call(post("/api/projects"), token, Map.of(
                "name", "Projeto atrasado",
                "deadline", LocalDate.now().minusDays(1).toString()), 422);
    }

    @Test
    @DisplayName("Usuário desativado não consegue mais fazer login nem usar o token antigo")
    void usuarioDesativado() throws Exception {
        String email = uniqueEmail();
        String token = registerAndLogin("Eva", email);

        call(delete("/api/users/me"), token, null, 204);

        call(post("/api/auth/login"), null, Map.of("email", email, "password", PASSWORD), 403);
        call(get("/api/projects"), token, null, 401);
    }

    @Test
    @DisplayName("Alterar senha: a senha antiga deixa de funcionar e a nova passa a valer")
    void alterarSenha() throws Exception {
        String email = uniqueEmail();
        String token = registerAndLogin("Fábio", email);

        call(patch("/api/users/me/password"), token,
                Map.of("currentPassword", PASSWORD, "newPassword", "NovaSenha99"), 204);

        call(post("/api/auth/login"), null, Map.of("email", email, "password", PASSWORD), 401);
        call(post("/api/auth/login"), null, Map.of("email", email, "password", "NovaSenha99"), 200);
    }

    @Test
    @DisplayName("Endpoints de administração: 403 para usuário comum, 200 para ADMIN")
    void apenasAdminListaUsuarios() throws Exception {
        String tokenComum = registerAndLogin("Gil", uniqueEmail());
        call(get("/api/admin/users"), tokenComum, null, 403);

        JsonNode login = call(post("/api/auth/login"), null,
                Map.of("email", "admin@test.com", "password", "Admin1234"), 200);
        String tokenAdmin = login.get("token").asText();

        JsonNode users = call(get("/api/admin/users"), tokenAdmin, null, 200);
        assertThat(users.get("totalElements").asInt()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Ordenação por propriedade inexistente retorna 400")
    void ordenacaoInvalida() throws Exception {
        String token = registerAndLogin("Ana", uniqueEmail());
        createProject(token);

        call(get("/api/projects?sort=naoExiste,asc"), token, null, 400);
    }

    // ---------------- métodos auxiliares ----------------

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.com";
    }

    private String registerAndLogin(String name, String email) throws Exception {
        call(post("/api/auth/register"), null, Map.of("name", name, "email", email, "password", PASSWORD), 201);
        JsonNode login = call(post("/api/auth/login"), null, Map.of("email", email, "password", PASSWORD), 200);
        return login.get("token").asText();
    }

    private long createProject(String token) throws Exception {
        JsonNode project = call(post("/api/projects"), token, Map.of(
                "name", "Projeto " + UUID.randomUUID(),
                "deadline", LocalDate.now().plusDays(30).toString()), 201);
        return project.get("id").asLong();
    }

    /** Executa a requisição, confere o status esperado e devolve o corpo como JSON (ou null se vazio). */
    private JsonNode call(MockHttpServletRequestBuilder request, String token, Object body, int expectedStatus)
            throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        MvcResult result = mockMvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return content.isBlank() ? null : objectMapper.readTree(content);
    }
}
