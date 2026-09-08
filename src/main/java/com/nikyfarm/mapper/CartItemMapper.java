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
        BigDecimal unitPrice = resolveEffectivePrice(variant);
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        Integer availableStock = variant != null ? variant.getStockQuantity() : null;

        return CartItemResponseDto.builder()
                .id(item.getId())
                .productVariantId(variant != null ? variant.getId() : null)
                .sku(variant != null ? variant.getSku() : null)
                .variantLabel(variant != null ? variant.getVariantLabel() : null)
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .primaryImageUrl(resolvePrimaryImageUrl(product))
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .lineTotal(lineTotal)
                .availableStock(availableStock)
                .stockInsufficient(availableStock != null && item.getQuantity() > availableStock)
                .addedAt(item.getAddedAt())
                .build();
    }

    /**
     * Row for the admin cart-items table (see admin/cart-items/list.html), which lists
     * items across every user's cart, so — unlike the customer-facing DTO — this needs
     * the owning user's id on every row. Field is named "createdAt" (not "addedAt") to
     * match the existing template.
     */
    public static CartItemAdminViewDto toAdminViewDto(CartItem item) {
        if (item == null) {
            return null;
        }

        ProductVariant variant = item.getProductVariant();
        Cart cart = item.getCart();
        BigDecimal unitPrice = resolveEffectivePrice(variant);
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        Long userId = (cart != null && cart.getUser() != null) ? cart.getUser().getId() : null;

        return CartItemAdminViewDto.builder()
                .id(item.getId())
                .userId(userId)
                .variantLabel(variant != null ? variant.getVariantLabel() : null)
                .sku(variant != null ? variant.getSku() : null)
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .lineTotal(lineTotal)
                .createdAt(item.getAddedAt())
                .build();
    }

    /**
     * "Charge the discounted price if present" — the same rule used elsewhere in this
     * project (e.g. ProductResponseDto). Falls back to `price` when `discountedPrice` is
     * null, and to zero if the variant itself is missing.
     */
    private static BigDecimal resolveEffectivePrice(ProductVariant variant) {
        if (variant == null) {
            return BigDecimal.ZERO;
        }
        if (variant.getDiscountedPrice() != null) {
            return BigDecimal.valueOf(variant.getDiscountedPrice());
        }
        return variant.getPrice() != null ? BigDecimal.valueOf(variant.getPrice()) : BigDecimal.ZERO;
    }

    /**
     * Prefers the image flagged isPrimary; falls back to the lowest sortOrder image if
     * none is flagged, and to null if the product has no images at all.
     *
     * NOTE: adjust getter names here (getIsPrimary/getSortOrder/getImageUrl) if they
     * differ from the actual ProductImage entity — this file was written from the field
     * list in the project docs, not the entity source itself.
     */
    private static String resolvePrimaryImageUrl(Product product) {
        if (product == null || product.getProductImages() == null || product.getProductImages().isEmpty()) {
            return null;
        }
        return product.getProductImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .findFirst()
                .or(() -> product.getProductImages().stream()
                        .min(Comparator.comparing(ProductImage::getSortOrder)))
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }
}
