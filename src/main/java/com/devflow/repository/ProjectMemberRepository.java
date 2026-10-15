package com.devflow.repository;

import com.devflow.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    List<ProjectMember> findByProjectId(Long projectId);

    long countByProjectId(Long projectId);

    long countByUserId(Long userId);
}
