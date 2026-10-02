package com.nikyfarm.dto;

public record ReviewEligibilityResponseDto(
        boolean eligible,
        boolean hasReviewed) {
}
