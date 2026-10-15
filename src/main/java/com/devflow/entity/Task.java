package com.devflow.entity;

import com.devflow.enums.TaskPriority;
import com.devflow.enums.TaskStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_tasks_project", columnList = "project_id"),
        @Index(name = "idx_tasks_assignee", columnList = "assignee_id"),
        @Index(name = "idx_tasks_status", columnList = "status"),
        @Index(name = "idx_tasks_deadline", columnList = "deadline")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIA;

    /** Responsável pela tarefa (opcional, mas obrigatório para concluí-la). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", foreignKey = @ForeignKey(name = "fk_tasks_assignee"))
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tasks_project"))
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tasks_created_by"))
    private User createdBy;

    private LocalDate deadline;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Relacionamento N:N com Tag, usando a tabela intermediária task_tags. */
    @ManyToMany
    @JoinTable(name = "task_tags",
            joinColumns = @JoinColumn(name = "task_id", foreignKey = @ForeignKey(name = "fk_task_tags_task")),
            inverseJoinColumns = @JoinColumn(name = "tag_id", foreignKey = @ForeignKey(name = "fk_task_tags_tag")))
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Uma tarefa está atrasada quando o prazo já passou e ela ainda não foi concluída. */
    public boolean isOverdue() {
        return deadline != null
                && status != TaskStatus.CONCLUIDA
                && deadline.isBefore(LocalDate.now());
    }
}
