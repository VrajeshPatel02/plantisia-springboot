package com.nikyfarm.dto;

import java.math.BigDecimal;

public record ProductVariantResponseDto(
        Long id,
        Long productId,
        String variantLabel,
        String size,
        String colour,
        BigDecimal price,
        BigDecimal discountedPrice,
        Integer stockQuantity,
        String sku,
        Boolean isDefault,
        Boolean isActive) {}
