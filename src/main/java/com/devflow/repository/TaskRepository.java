package com.devflow.repository;

import com.devflow.entity.Task;
import com.devflow.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    // ---- contagens por projeto ----
    long countByProjectId(Long projectId);

    long countByProjectIdAndStatus(Long projectId, TaskStatus status);

    long countByProjectIdAndStatusNot(Long projectId, TaskStatus status);

    // ---- contagens por usuário ----
    long countByAssigneeId(Long userId);

    long countByAssigneeIdAndStatus(Long userId, TaskStatus status);

    // ---- contagens para o dashboard (vários projetos) ----
    long countByProjectIdIn(Collection<Long> projectIds);

    long countByProjectIdInAndStatus(Collection<Long> projectIds, TaskStatus status);

    long countByProjectIdInAndDeadlineBeforeAndStatusNot(Collection<Long> projectIds, LocalDate date, TaskStatus status);

    @Query("select t.priority, count(t) from Task t where t.project.id in :projectIds group by t.priority")
    List<Object[]> countGroupedByPriority(@Param("projectIds") Collection<Long> projectIds);

    @Query("select t.status, count(t) from Task t where t.project.id in :projectIds group by t.status")
    List<Object[]> countGroupedByStatus(@Param("projectIds") Collection<Long> projectIds);

    /** Retorna linhas [userId, nome, totalAtribuidas, totalConcluidas]. */
    @Query("""
            select u.id, u.name, count(t), sum(case when t.status = :done then 1 else 0 end)
            from Task t join t.assignee u
            where t.project.id in :projectIds
            group by u.id, u.name
            """)
    List<Object[]> findProductivityByAssignee(@Param("projectIds") Collection<Long> projectIds,
                                              @Param("done") TaskStatus done);

    /** Quando um membro sai do projeto, suas tarefas ficam sem responsável. */
    @Modifying(flushAutomatically = true)
    @Query("update Task t set t.assignee = null where t.project.id = :projectId and t.assignee.id = :userId")
    int unassignUserFromProject(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
