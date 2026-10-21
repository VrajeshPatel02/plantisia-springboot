package com.nikyfarm.mapper;

import com.nikyfarm.dto.CartItemAdminViewDto;
import com.nikyfarm.dto.CartItemResponseDto;
import com.nikyfarm.entity.Cart;
import com.nikyfarm.entity.CartItem;
import com.nikyfarm.entity.Product;
import com.nikyfarm.entity.ProductImage;
import com.nikyfarm.entity.ProductVariant;

import java.math.BigDecimal;
import java.util.Comparator;

public final class CartItemMapper {

    private CartItemMapper() {
    }

    public static CartItemResponseDto toResponseDto(CartItem item) {
        if (item == null) {
            return null;
        }

        ProductVariant variant = item.getProductVariant();
        Product product = variant != null ? variant.getProduct() : null;

        BigDecimal originalPrice = resolveOriginalPrice(variant);
        BigDecimal unitPrice = resolveEffectivePrice(variant);
        BigDecimal lineTotal =
                unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

        Integer availableStock =
                variant != null ? variant.getStockQuantity() : null;

        return CartItemResponseDto.builder()
                .id(item.getId())
                .productVariantId(variant != null ? variant.getId() : null)
                .sku(variant != null ? variant.getSku() : null)
                .variantLabel(variant != null ? variant.getVariantLabel() : null)
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .primaryImageUrl(resolvePrimaryImageUrl(product))
                .originalPrice(originalPrice)
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .lineTotal(lineTotal)
                .availableStock(availableStock)
                .stockInsufficient(
                        availableStock != null
                                && item.getQuantity() > availableStock
                )
                .addedAt(item.getAddedAt())
                .build();
    }

    /**
     * Row for the admin cart-items table.
     *
     * Unlike the customer-facing DTO, this includes the owning user's id.
     */
    public static CartItemAdminViewDto toAdminViewDto(CartItem item) {
        if (item == null) {
            return null;
        }

        ProductVariant variant = item.getProductVariant();
        Cart cart = item.getCart();

        BigDecimal unitPrice = resolveEffectivePrice(variant);
        BigDecimal lineTotal =
                unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

        Long userId =
                (cart != null && cart.getUser() != null)
                        ? cart.getUser().getId()
                        : null;

        return CartItemAdminViewDto.builder()
                .id(item.getId())
                .userId(userId)
                .variantLabel(
                        variant != null ? variant.getVariantLabel() : null
                )
                .sku(variant != null ? variant.getSku() : null)
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .lineTotal(lineTotal)
                .createdAt(item.getAddedAt())
                .build();
    }

    /**
     * Returns the regular variant price before discount.
     * A missing variant or price is represented as zero in the response.
     */
    private static BigDecimal resolveOriginalPrice(ProductVariant variant) {
        if (variant == null || variant.getPrice() == null) {
            return BigDecimal.ZERO;
        }

        return variant.getPrice();
    }

    /**
     * Uses discountedPrice when available.
     * Falls back to regular price.
     * Falls back to zero if the variant is missing or has no price.
     */
    private static BigDecimal resolveEffectivePrice(ProductVariant variant) {
        if (variant == null) {
            return BigDecimal.ZERO;
        }

        if (variant.getDiscountedPrice() != null) {
            return variant.getDiscountedPrice();
        }

        return variant.getPrice() != null
                ? variant.getPrice()
                : BigDecimal.ZERO;
    }

    /**
     * Resolves the primary product image.
     *
     * Priority:
     * 1. Image explicitly marked as primary.
     * 2. Image with the lowest non-null sortOrder.
     * 3. If sortOrder is null for all images, falls back safely to
     *    the first available image.
     * 4. null when the product has no images.
     */
    private static String resolvePrimaryImageUrl(Product product) {
        if (product == null
                || product.getProductImages() == null
                || product.getProductImages().isEmpty()) {
            return null;
        }

        return product.getProductImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .findFirst()
                .or(() ->
                        product.getProductImages().stream()
                                .min(
                                        Comparator.comparing(
                                                ProductImage::getSortOrder,
                                                Comparator.nullsLast(
                                                        Integer::compareTo
                                                )
                                        )
                                )
                )
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }
}