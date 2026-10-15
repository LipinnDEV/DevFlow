package com.devflow.support;

import com.devflow.entity.Project;
import com.devflow.entity.Task;
import com.devflow.entity.User;
import com.devflow.enums.ProjectStatus;
import com.devflow.enums.Role;
import com.devflow.enums.TaskPriority;
import com.devflow.enums.TaskStatus;

import java.time.LocalDate;

/** Cria objetos de domínio para os testes, evitando repetição. */
public final class TestFactory {

    private TestFactory() {
    }

    public static User user(Long id, String name, Role role) {
        return User.builder()
                .id(id)
                .name(name)
                .email(name.toLowerCase() + "@test.com")
                .password("hash")
                .role(role)
                .active(true)
                .build();
    }

    public static Project project(Long id, User owner, LocalDate deadline) {
        return Project.builder()
                .id(id)
                .name("Projeto " + id)
                .status(ProjectStatus.PLANEJADO)
                .deadline(deadline)
                .owner(owner)
                .build();
    }

    public static Task task(Long id, Project project, User createdBy, User assignee, TaskStatus status) {
        return Task.builder()
                .id(id)
                .title("Tarefa " + id)
                .status(status)
                .priority(TaskPriority.MEDIA)
                .project(project)
                .createdBy(createdBy)
                .assignee(assignee)
                .build();
    }
}
