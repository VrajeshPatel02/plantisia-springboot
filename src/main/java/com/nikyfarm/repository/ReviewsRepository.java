package com.nikyfarm.repository;

import com.nikyfarm.entity.Reviews;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewsRepository extends JpaRepository<Reviews, Long> {
    List<Reviews> findByProductId(Long productId);

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    boolean existsByReviewImageId(Long reviewImageId);
}
