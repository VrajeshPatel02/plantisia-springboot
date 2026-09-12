package com.nikyfarm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * No {@code isVerifiedPurchase} field -- the server always sets that (and currently always to
 * false, since there's no Order-tracking service yet to verify against); a reviewer can never claim
 * their own purchase was verified.
 */
@Data
public class ReviewRequestDto {

    /**
     * Set by the controller from the path ("/api/products/{productId}/reviews"), not the request
     * body.
     */
    private Long productId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Short rating;

    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @Size(max = 2000, message = "Comment must be at most 2000 characters")
    private String comment;

    /** Optional -- upload via POST /api/review-images first, then reference the returned id here. */
    private Long reviewImageId;
}
