package com.nikyfarm.dto;

import com.nikyfarm.entity.User;
import java.time.LocalDateTime;

public record AuthResponseDto(
        String token,
        String tokenType,
        Long userId,
        String name,
        String email,
        User.Role role,
        LocalDateTime expiresAt) {}
