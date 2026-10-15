package com.devflow.entity;

import com.devflow.enums.ActivityType;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Registro de auditoria. O usuário é uma chave estrangeira normal; já projectId e taskId são
 * referências "soft" (sem FK) de propósito: assim o histórico continua existindo mesmo depois
 * que o projeto ou a tarefa forem excluídos.
 */
@Entity
@Table(name = "activities", indexes = {
        @Index(name = "idx_activities_user", columnList = "user_id"),
        @Index(name = "idx_activities_project", columnList = "project_id"),
        @Index(name = "idx_activities_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 40)
    private ActivityType type;

    @Column(nullable = false, length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_activities_user"))
    private User user;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "task_id")
    private Long taskId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
