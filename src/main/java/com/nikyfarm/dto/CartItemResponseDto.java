package com.nikyfarm.dto;

import java.time.LocalDateTime;

public record CartItemResponseDto(
        Long id,
        Long userId,
        Long productVariantId,
        String variantLabel,
        String sku,
        Double unitPrice,
        Integer quantity,
        Double lineTotal,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
