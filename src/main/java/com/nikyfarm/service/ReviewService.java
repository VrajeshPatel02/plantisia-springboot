package com.nikyfarm.service;

import com.nikyfarm.dto.ReviewRequestDto;
import com.nikyfarm.dto.ReviewResponseDto;
import com.nikyfarm.dto.ReviewUpdateRequestDto;

import java.util.List;

public interface ReviewService {

    ReviewResponseDto createReview(Long userId, ReviewRequestDto dto);

    ReviewResponseDto updateReview(Long reviewId, Long currentUserId, boolean isAdmin, ReviewUpdateRequestDto dto);

    List<ReviewResponseDto> getReviewsByProduct(Long productId);

    /** Admin moderation list — every review across every product. */
    List<ReviewResponseDto> getAllReviews();

    ReviewResponseDto getById(Long reviewId);

    void deleteReview(Long reviewId, Long currentUserId, boolean isAdmin);

    /** Detaches and deletes the review's image (Cloudinary + row), leaving the review itself intact. */
    void removeImage(Long reviewId);
}
