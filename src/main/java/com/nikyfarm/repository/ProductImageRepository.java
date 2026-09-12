package com.nikyfarm.repository;

import com.nikyfarm.entity.ProductImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    long countByProductId(Long productId);

    // General images only (variant IS NULL) — e.g. main gallery before a variant is picked
    List<ProductImage> findByProductIdAndVariantIsNull(Long productId);

    // Images specific to one variant — e.g. swap gallery when customer picks "Sage Green"
    List<ProductImage> findByVariantId(Long variantId);

    // Everything for a product regardless of variant — e.g. admin panel showing all uploads
    List<ProductImage> findByProductId(Long productId);
}
