package com.nikyfarm.mapper;

import com.nikyfarm.dto.ProductVariantRequestDto;
import com.nikyfarm.dto.ProductVariantResponseDto;
import com.nikyfarm.entity.ProductVariant;

public final class ProductVariantMapper {

    private ProductVariantMapper() {
    }

    public static ProductVariantResponseDto toResponseDto(ProductVariant variant) {
        return new ProductVariantResponseDto(
                variant.getId(),
                variant.getProduct() != null ? variant.getProduct().getId() : null,
                variant.getVariantLabel(),
                variant.getSize(),
                variant.getColour(),
                variant.getPrice(),
                variant.getDiscountedPrice(),
                variant.getStockQuantity(),
                variant.getSku(),
                variant.getIsDefault(),
                variant.getIsActive()
        );
    }

    /** Pre-fills an edit form with the current values of an existing variant. */
    public static ProductVariantRequestDto toRequestDto(ProductVariantResponseDto dto) {
        ProductVariantRequestDto request = new ProductVariantRequestDto();
        request.setVariantLabel(dto.variantLabel());
        request.setSize(dto.size());
        request.setColour(dto.colour());
        request.setPrice(dto.price());
        request.setDiscountedPrice(dto.discountedPrice());
        request.setStockQuantity(dto.stockQuantity());
        request.setSku(dto.sku());
        request.setIsDefault(dto.isDefault());
        request.setIsActive(dto.isActive());
        return request;
    }
}
