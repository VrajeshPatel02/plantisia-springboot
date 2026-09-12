package com.nikyfarm.dto;

import java.time.LocalDateTime;

public record ReviewResponseDto(
        Long id,
        Long productId,
        String productName,
        Long userId,
        String userName,
        Short rating,
        String title,
        String comment,
        Boolean isVerifiedPurchase,
        Long reviewImageId,
        String reviewImageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
