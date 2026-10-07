package com.nikyfarm.dto;

public record ReviewMediaResponseDto(
        Long id,
        String mediaType,
        String mediaUrl,
        String thumbnailUrl,
        Integer sortOrder) {
}