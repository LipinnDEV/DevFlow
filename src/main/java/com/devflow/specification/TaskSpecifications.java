package com.devflow.specification;

import com.devflow.entity.ProjectMember;
import com.devflow.entity.Task;
import com.devflow.enums.TaskPriority;
import com.devflow.enums.TaskStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/** Filtros dinâmicos para a listagem de tarefas. Filtro nulo = ignorado. */
public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> inProject(Long projectId) {
        return (root, query, cb) -> projectId == null ? null : cb.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Task> hasPriority(TaskPriority priority) {
        return (root, query, cb) -> priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Task> hasAssignee(Long assigneeId) {
        return (root, query, cb) -> assigneeId == null ? null : cb.equal(root.get("assignee").get("id"), assigneeId);
    }

    public static Specification<Task> titleContains(String text) {
        return (root, query, cb) -> (text == null || text.isBlank())
                ? null
                : cb.like(cb.lower(root.get("title")), "%" + text.trim().toLowerCase() + "%");
    }

    public static Specification<Task> hasTag(String tagName) {
        return (root, query, cb) -> {
            if (tagName == null || tagName.isBlank()) {
                return null;
            }
            Join<Object, Object> tags = root.join("tags");
            return cb.equal(cb.lower(tags.get("name")), tagName.trim().toLowerCase());
        };
    }

    /** Só filtra quando overdue = true: prazo vencido e tarefa ainda não concluída. */
    public static Specification<Task> isOverdue(Boolean overdue) {
        return (root, query, cb) -> {
            if (!Boolean.TRUE.equals(overdue)) {
                return null;
            }
            return cb.and(
                    cb.lessThan(root.<LocalDate>get("deadline"), LocalDate.now()),
                    cb.notEqual(root.get("status"), TaskStatus.CONCLUIDA));
        };
    }

    /** Tarefas de projetos dos quais o usuário participa. */
    public static Specification<Task> inProjectsOf(Long userId) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<ProjectMember> member = subquery.from(ProjectMember.class);
            subquery.select(member.<Long>get("id"))
                    .where(cb.equal(member.get("project"), root.get("project")),
                            cb.equal(member.get("user").get("id"), userId));
            return cb.exists(subquery);
        };
    }
}
