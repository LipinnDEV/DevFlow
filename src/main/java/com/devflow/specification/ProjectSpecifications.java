package com.devflow.specification;

import com.devflow.entity.Project;
import com.devflow.entity.ProjectMember;
import com.devflow.enums.ProjectStatus;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros dinâmicos para a listagem de projetos.
 * Cada método devolve null quando o filtro não foi informado (e o Spring simplesmente o ignora).
 */
public final class ProjectSpecifications {

    private ProjectSpecifications() {
    }

    public static Specification<Project> hasStatus(ProjectStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Project> nameContains(String name) {
        return (root, query, cb) -> (name == null || name.isBlank())
                ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%");
    }

    /** Projetos dos quais o usuário participa (existe uma linha em project_members). */
    public static Specification<Project> hasMember(Long userId) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<ProjectMember> member = subquery.from(ProjectMember.class);
            subquery.select(member.<Long>get("id"))
                    .where(cb.equal(member.get("project"), root),
                            cb.equal(member.get("user").get("id"), userId));
            return cb.exists(subquery);
        };
    }
}
