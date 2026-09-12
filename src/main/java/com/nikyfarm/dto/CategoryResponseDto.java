package com.nikyfarm.dto;

import java.time.LocalDateTime;

public record CategoryResponseDto(
        Long id,
        String name,
        String slug,
        String description,
        String imageUrl,
        Integer sortOrder,
        Boolean isActive,
        Long parentId,
        String parentName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
