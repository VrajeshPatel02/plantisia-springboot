package com.nikyfarm.dto;

public record ProductImageResponseDto(
        Long id,
        Long productId,
        Long variantId,
        String variantLabel,
        String imageUrl,
        String altText,
        Integer sortOrder,
        Boolean isPrimary) {}
