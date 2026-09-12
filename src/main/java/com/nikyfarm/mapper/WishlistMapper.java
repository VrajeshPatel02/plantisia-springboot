package com.nikyfarm.mapper;

import com.nikyfarm.dto.WishlistResponseDto;
import com.nikyfarm.entity.Product;
import com.nikyfarm.entity.ProductImage;
import com.nikyfarm.entity.Wishlist;

public final class WishlistMapper {

    private WishlistMapper() {}

    public static WishlistResponseDto toResponseDto(Wishlist wishlist) {
        Product product = wishlist.getProduct();
        String primaryImageUrl = product == null || product.getProductImages() == null
                ? null
                : product.getProductImages().stream()
                        .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                        .map(ProductImage::getImageUrl)
                        .findFirst()
                        .orElse(null);

        return new WishlistResponseDto(
                wishlist.getId(),
                wishlist.getUser() != null ? wishlist.getUser().getId() : null,
                product != null ? product.getId() : null,
                product != null ? product.getName() : null,
                product != null ? product.getSlug() : null,
                primaryImageUrl,
                wishlist.getCreatedAt());
    }
}
