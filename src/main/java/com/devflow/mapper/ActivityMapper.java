package com.devflow.mapper;

import com.devflow.dto.activity.ActivityResponse;
import com.devflow.entity.Activity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityMapper {

    private final UserMapper userMapper;

    public ActivityResponse toResponse(Activity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getType(),
                activity.getDescription(),
                userMapper.toSummary(activity.getUser()),
                activity.getProjectId(),
                activity.getTaskId(),
                activity.getCreatedAt());
    }
}
