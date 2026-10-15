package com.devflow.service;

import com.devflow.dto.activity.ActivityResponse;
import com.devflow.dto.common.PageResponse;
import com.devflow.entity.Activity;
import com.devflow.entity.User;
import com.devflow.enums.ActivityType;
import com.devflow.mapper.ActivityMapper;
import com.devflow.repository.ActivityRepository;
import com.devflow.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registra e consulta o histórico de atividades (quem fez o quê e quando). */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityMapper activityMapper;
    private final CurrentUserService currentUserService;
    private final ProjectAccessService projectAccessService;

    /** Participa da transação de quem chamou: se a operação principal falhar, a atividade também é desfeita. */
    @Transactional
    public void register(ActivityType type, String description, User user, Long projectId, Long taskId) {
        Activity activity = Activity.builder()
                .type(type)
                .description(description)
                .user(user)
                .projectId(projectId)
                .taskId(taskId)
                .build();
        activityRepository.save(activity);
        log.debug("Atividade registrada: {}", description);
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> listMine(Pageable pageable) {
        User current = currentUserService.getCurrentUser();
        Page<Activity> page = activityRepository.findByUserId(current.getId(), pageable);
        return PageResponse.from(page.map(activityMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> listByProject(Long projectId, Pageable pageable) {
        User current = currentUserService.getCurrentUser();
        projectAccessService.getProjectForMember(projectId, current);
        Page<Activity> page = activityRepository.findByProjectId(projectId, pageable);
        return PageResponse.from(page.map(activityMapper::toResponse));
    }
}
