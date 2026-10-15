package com.devflow.mapper;

import com.devflow.dto.common.UserSummary;
import com.devflow.dto.user.UserResponse;
import com.devflow.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt());
    }

    public UserSummary toSummary(User user) {
        return user == null ? null : new UserSummary(user.getId(), user.getName());
    }
}
