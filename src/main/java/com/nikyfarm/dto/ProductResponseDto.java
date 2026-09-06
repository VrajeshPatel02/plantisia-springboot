package com.nikyfarm.dto;

import java.time.LocalDateTime;

public record ProductResponseDto(
        Long id,
        String name,
        String slug,
        String shortDescription,
        String description,
        Double basePrice,
        Double discountedPrice,
        Float avgRating,
        Integer reviewCount,
        Boolean isActive,
        Long categoryId,
        String categoryName,
        String primaryImageUrl,
        int variantCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
