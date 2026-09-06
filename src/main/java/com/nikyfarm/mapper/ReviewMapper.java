package com.nikyfarm.mapper;

import com.nikyfarm.dto.ReviewResponseDto;
import com.nikyfarm.entity.Product;
import com.nikyfarm.entity.ReviewImage;
import com.nikyfarm.entity.Reviews;
import com.nikyfarm.entity.User;

public final class ReviewMapper {

    private ReviewMapper() {
    }

    public static ReviewResponseDto toResponseDto(Reviews review) {
        Product product = review.getProduct();
        User user = review.getUser();
        ReviewImage image = review.getReviewImage();
        return new ReviewResponseDto(
                review.getId(),
                product != null ? product.getId() : null,
                product != null ? product.getName() : null,
                user != null ? user.getId() : null,
                user != null ? user.getName() : null,
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getIsVerifiedPurchase(),
                image != null ? image.getId() : null,
                image != null ? image.getImageUrl() : null,
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
