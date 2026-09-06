package com.nikyfarm.dto;

public record ProductVariantResponseDto(
        Long id,
        Long productId,
        String variantLabel,
        String size,
        String colour,
        Double price,
        Double discountedPrice,
        Integer stockQuantity,
        String sku,
        Boolean isDefault,
        Boolean isActive
) {
}
