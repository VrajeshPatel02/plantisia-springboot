package com.nikyfarm.mapper;

import com.nikyfarm.dto.ProductRequestDto;
import com.nikyfarm.dto.ProductResponseDto;
import com.nikyfarm.entity.Product;
import com.nikyfarm.entity.ProductImage;

public final class ProductMapper {

    private ProductMapper() {}

    public static ProductResponseDto toResponseDto(Product product) {
        String primaryImageUrl = product.getProductImages() == null
                ? null
                : product.getProductImages().stream()
                        .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                        .map(ProductImage::getImageUrl)
                        .findFirst()
                        .orElse(null);

        return new ProductResponseDto(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getShortDescription(),
                product.getDescription(),
                product.getBasePrice(),
                product.getDiscountedPrice(),
                product.getAvgRating(),
                product.getReviewCount(),
                product.getIsActive(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                primaryImageUrl,
                product.getProductVariants() != null
                        ? product.getProductVariants().size()
                        : 0,
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

    /** Pre-fills an edit form with the current values of an existing product. */
    public static ProductRequestDto toRequestDto(ProductResponseDto dto) {
        ProductRequestDto request = new ProductRequestDto();
        request.setName(dto.name());
        request.setSlug(dto.slug());
        request.setShortDescription(dto.shortDescription());
        request.setDescription(dto.description());
        request.setBasePrice(dto.basePrice());
        request.setDiscountedPrice(dto.discountedPrice());
        request.setIsActive(dto.isActive());
        request.setCategoryId(dto.categoryId());
        return request;
    }
}
