package com.devflow.repository;

import com.devflow.entity.Project;
import com.devflow.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    long countByIdIn(Collection<Long> ids);

    long countByIdInAndStatus(Collection<Long> ids, ProjectStatus status);

    @Query("select pm.project.id from ProjectMember pm where pm.user.id = :userId")
    List<Long> findIdsByMemberId(@Param("userId") Long userId);

    @Query("select p.id from Project p")
    List<Long> findAllIds();
}
