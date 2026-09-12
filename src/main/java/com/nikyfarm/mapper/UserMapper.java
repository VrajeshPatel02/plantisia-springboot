package com.nikyfarm.mapper;

import com.nikyfarm.dto.UserResponseDto;
import com.nikyfarm.entity.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponseDto toResponseDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getIsActive(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
