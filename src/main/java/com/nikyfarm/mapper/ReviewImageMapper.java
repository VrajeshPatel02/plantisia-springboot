package com.nikyfarm.mapper;

import com.nikyfarm.dto.ReviewImageResponseDto;
import com.nikyfarm.entity.ReviewImage;

public final class ReviewImageMapper {

    private ReviewImageMapper() {
    }

    public static ReviewImageResponseDto toResponseDto(ReviewImage image) {
        return new ReviewImageResponseDto(image.getId(), image.getImageUrl());
    }
}
