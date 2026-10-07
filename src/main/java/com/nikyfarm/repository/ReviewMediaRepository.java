package com.nikyfarm.repository;

import com.nikyfarm.entity.ReviewMedia;
import com.nikyfarm.entity.ReviewMediaType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, Long> {

    List<ReviewMedia> findByReviewIdOrderBySortOrderAscIdAsc(Long reviewId);

    long countByReviewId(Long reviewId);

    long countByReviewIdAndMediaType(Long reviewId, ReviewMediaType mediaType);
}
