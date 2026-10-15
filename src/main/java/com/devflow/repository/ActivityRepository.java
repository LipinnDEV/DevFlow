package com.devflow.repository;

import com.devflow.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    Page<Activity> findByProjectId(Long projectId, Pageable pageable);

    Page<Activity> findByUserId(Long userId, Pageable pageable);
}
