package com.nikyfarm.mapper;

import com.nikyfarm.dto.ProductRequestDto;
import com.nikyfarm.dto.ProductResponseDto;
import com.nikyfarm.entity.Product;
import com.nikyfarm.entity.ProductImage;
import com.nikyfarm.entity.ProductVariant;

import java.util.Comparator;
import java.util.List;

public final class ProductMapper {

    private ProductMapper() {
    }

    /**
     * Maps a product using the values currently stored on Product.
     * <p>
     * Used for create/update responses where review aggregation is not
     * required to build the response.
     */
    public static ProductResponseDto toResponseDto(Product product) {
        return toResponseDto(
                product,
                product.getAvgRating(),
                product.getReviewCount()
        );
    }

    /**
     * Maps a product using review information calculated by the service.
     * <p>
     * Review data is deliberately supplied to the mapper instead of being
     * queried from the mapper. This keeps repository/service responsibilities
     * separate.
     */
    public static ProductResponseDto toResponseDto(
            Product product,
            Float avgRating,
            Integer reviewCount
    ) {
        String primaryImageUrl = resolveDisplayImageUrl(product);

        return new ProductResponseDto(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getShortDescription(),
                product.getDescription(),
                product.getBasePrice(),
                product.getDiscountedPrice(),
                avgRating,
                reviewCount,
                product.getIsActive(),
                product.getCategory() != null
                        ? product.getCategory().getId()
                        : null,
                product.getCategory() != null
                        ? product.getCategory().getName()
                        : null,
                primaryImageUrl,
                product.getProductVariants() != null
                        ? product.getProductVariants().size()
                        : 0,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    /**
     * Resolves the image used as the product's primary/display image.
     * <p>
     * Priority:
     * 1. First image (lowest sortOrder) belonging to the default variant.
     * 2. First product-level image (lowest sortOrder) as fallback.
     * <p>
     * sortOrder is the source of truth for display order.
     */
    private static String resolveDisplayImageUrl(Product product) {
        List<ProductImage> images = product.getProductImages();

        if (images == null || images.isEmpty()) {
            return null;
        }

        ProductVariant defaultVariant = product.getProductVariants() == null
                ? null
                : product.getProductVariants()
                .stream()
                .filter(variant -> Boolean.TRUE.equals(variant.getIsDefault()))
                .findFirst()
                .orElse(null);

        if (defaultVariant != null) {
            Long defaultVariantId = defaultVariant.getId();

            String defaultVariantImage = images.stream()
                    .filter(image -> image.getVariant() != null)
                    .filter(image ->
                            defaultVariantId.equals(image.getVariant().getId()))
                    .sorted(imageComparator())
                    .map(ProductImage::getImageUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .findFirst()
                    .orElse(null);

            if (defaultVariantImage != null) {
                return defaultVariantImage;
            }
        }

        return images.stream()
                .filter(image -> image.getVariant() == null)
                .sorted(imageComparator())
                .map(ProductImage::getImageUrl)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);
    }

    private static Comparator<ProductImage> imageComparator() {
        return Comparator
                .comparing(
                        ProductImage::getSortOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())
                )
                .thenComparing(
                        ProductImage::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())
                );
    }

    /**
     * Pre-fills an edit form with the current values of an existing product.
     */
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
