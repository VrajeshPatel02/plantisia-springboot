package com.nikyfarm.mapper;

import com.nikyfarm.dto.ReviewMediaResponseDto;
import com.nikyfarm.dto.ReviewResponseDto;
import com.nikyfarm.entity.Product;
import com.nikyfarm.entity.ReviewImage;
import com.nikyfarm.entity.ReviewMedia;
import com.nikyfarm.entity.Reviews;
import com.nikyfarm.entity.User;

import java.util.List;

public final class ReviewMapper {

    private ReviewMapper() {}

    public static ReviewResponseDto toResponseDto(Reviews review) {
        Product product = review.getProduct();
        User user = review.getUser();
        ReviewImage image = review.getReviewImage();

        List<ReviewMediaResponseDto> media =
                review.getMedia() == null
                        ? List.of()
                        : review.getMedia().stream()
                        .map(ReviewMapper::toMediaResponseDto)
                        .toList();

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

                // Legacy image fields
                image != null ? image.getId() : null,
                image != null ? image.getImageUrl() : null,

                // New media collection
                media,

                review.getCreatedAt(),
                review.getUpdatedAt());
    }

    private static ReviewMediaResponseDto toMediaResponseDto(ReviewMedia media) {
        return new ReviewMediaResponseDto(
                media.getId(),
                media.getMediaType() != null
                        ? media.getMediaType().name()
                        : null,
                media.getMediaUrl(),
                media.getThumbnailUrl(),
                media.getSortOrder());
    }
}