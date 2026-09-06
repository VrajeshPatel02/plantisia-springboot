package com.nikyfarm.repository;

import com.nikyfarm.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);
    List<ProductVariant> findByProductId(Long productId);

    @Modifying
    @Query("update ProductVariant v set v.isDefault = false where v.product.id = :productId")
    void clearDefaultForProduct(@Param("productId") Long productId);

    @Modifying
    @Query("update ProductVariant v set v.isDefault = false where v.product.id = :productId and v.id <> :excludeId")
    void clearDefaultForProductExcept(@Param("productId") Long productId, @Param("excludeId") Long excludeId);
}
