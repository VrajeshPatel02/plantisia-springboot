package com.nikyfarm.repository;

import com.nikyfarm.entity.ReviewImage;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    Optional<ReviewImage> findByIdAndUserIdAndProductId(
            Long id,
            Long userId,
            Long productId);
}
