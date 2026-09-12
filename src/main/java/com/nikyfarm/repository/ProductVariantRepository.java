package com.nikyfarm.repository;

import com.nikyfarm.entity.ProductVariant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    List<ProductVariant> findByProductId(Long productId);

    Optional<ProductVariant> findFirstByProductIdAndIsActiveTrueOrderByIdAsc(Long productId);

    @Modifying
    @Query(
            """
            update ProductVariant v
            set v.stockQuantity =
                    v.stockQuantity - :quantity
            where v.id = :variantId
            and v.stockQuantity >= :quantity
            """)
    int deductStockIfAvailable(@Param("variantId") Long variantId, @Param("quantity") Integer quantity);

    @Modifying
    @Query(
            """
            update ProductVariant v
            set v.stockQuantity =
                    v.stockQuantity + :quantity
            where v.id = :variantId
            """)
    int restoreStock(@Param("variantId") Long variantId, @Param("quantity") Integer quantity);

    @Modifying
    @Query(
            """
            update ProductVariant v
            set v.isDefault = false
            where v.product.id = :productId
            """)
    void clearDefaultForProduct(@Param("productId") Long productId);

    @Modifying
    @Query(
            """
            update ProductVariant v
            set v.isDefault = false
            where v.product.id = :productId
            and v.id <> :excludeId
            """)
    void clearDefaultForProductExcept(@Param("productId") Long productId, @Param("excludeId") Long excludeId);
}
