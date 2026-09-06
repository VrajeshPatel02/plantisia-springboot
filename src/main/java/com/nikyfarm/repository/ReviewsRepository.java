package com.nikyfarm.repository;

import com.nikyfarm.entity.Reviews;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewsRepository extends JpaRepository<Reviews, Long> {
    List<Reviews> findByProductId(Long productId);
    boolean existsByProductIdAndUserId(Long productId, Long userId);
    boolean existsByReviewImageId(Long reviewImageId);
}
