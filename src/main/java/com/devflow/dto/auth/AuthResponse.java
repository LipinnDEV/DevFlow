package com.devflow.dto.auth;

import com.devflow.dto.user.UserResponse;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        UserResponse user
) {
}
