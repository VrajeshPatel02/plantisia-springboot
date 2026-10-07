package com.nikyfarm.dto;

import java.time.LocalDateTime;
import java.util.List;

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

        // Legacy single-image fields.
        // Kept temporarily for backward compatibility.
        Long reviewImageId,
        String reviewImageUrl,

        // New multiple-media collection.
        List<ReviewMediaResponseDto> media,

        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}