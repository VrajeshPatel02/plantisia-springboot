package com.nikyfarm.mapper;

import com.nikyfarm.dto.CartItemResponseDto;
import com.nikyfarm.entity.CartItems;
import com.nikyfarm.entity.ProductVariant;

public final class CartItemMapper {

    private CartItemMapper() {
    }

    public static CartItemResponseDto toResponseDto(CartItems cartItem) {
        ProductVariant variant = cartItem.getProductVariant();
        Double unitPrice = variant == null ? null
                : variant.getDiscountedPrice() != null ? variant.getDiscountedPrice() : variant.getPrice();
        Double lineTotal = unitPrice != null && cartItem.getQuantity() != null
                ? unitPrice * cartItem.getQuantity()
                : null;

        return new CartItemResponseDto(
                cartItem.getId(),
                cartItem.getUser() != null ? cartItem.getUser().getId() : null,
                variant != null ? variant.getId() : null,
                variant != null ? variant.getVariantLabel() : null,
                variant != null ? variant.getSku() : null,
                unitPrice,
                cartItem.getQuantity(),
                lineTotal,
                cartItem.getCreatedAt(),
                cartItem.getUpdatedAt()
        );
    }
}
