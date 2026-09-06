package com.nikyfarm.mapper;

import com.nikyfarm.dto.ProductImageResponseDto;
import com.nikyfarm.entity.ProductImage;
import com.nikyfarm.entity.ProductVariant;

public final class ProductImageMapper {

    private ProductImageMapper() {
    }

    public static ProductImageResponseDto toResponseDto(ProductImage image) {
        ProductVariant variant = image.getVariant();
        return new ProductImageResponseDto(
                image.getId(),
                image.getProduct() != null ? image.getProduct().getId() : null,
                variant != null ? variant.getId() : null,
                variant != null ? variant.getVariantLabel() : null,
                image.getImageUrl(),
                image.getAltText(),
                image.getSortOrder(),
                image.getIsPrimary()
        );
    }
}
