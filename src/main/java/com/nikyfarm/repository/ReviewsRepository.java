package com.nikyfarm.repository;

import com.nikyfarm.entity.Reviews;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewsRepository extends JpaRepository<Reviews, Long> {

    List<Reviews> findByProductId(Long productId);

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    boolean existsByReviewImageId(Long reviewImageId);

    /**
     * Returns one aggregate row per product:
     * <p>
     * [0] product id
     * [1] average rating
     * [2] review count
     * <p>
     * The query is used for product listings so the application does not
     * execute one review query per product.
     */
    @Query("""
            SELECT r.product.id, AVG(r.rating), COUNT(r.id)
            FROM Reviews r
            WHERE r.product.id IN :productIds
            GROUP BY r.product.id
            """)
    List<Object[]> findRatingSummaryByProductIds(
            @Param("productIds") List<Long> productIds
    );
}
