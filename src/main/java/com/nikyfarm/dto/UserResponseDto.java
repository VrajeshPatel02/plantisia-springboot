package com.nikyfarm.dto;

import com.nikyfarm.entity.User;

import java.time.LocalDateTime;

/** Never includes {@code passwordHash} -- this is what every /api/users endpoint returns. */
public record UserResponseDto(
        Long id,
        String name,
        String email,
        User.Role role,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
