package com.nikyfarm.dto;

import java.time.LocalDateTime;

public record WishlistResponseDto(
        Long id,
        Long userId,
        Long productId,
        String productName,
        String productSlug,
        String primaryImageUrl,
        LocalDateTime createdAt) {}
