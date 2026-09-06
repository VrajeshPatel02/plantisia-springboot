package com.nikyfarm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * No {@code productId} -- a review can't be reassigned to a different product, only
 * edited in place. {@code isVerifiedPurchase} is present but only has any effect when the
 * caller is an admin (the service silently ignores it for a customer editing their own
 * review, rather than rejecting the request over one harmless field).
 */
@Data
public class ReviewUpdateRequestDto {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Short rating;

    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @Size(max = 2000, message = "Comment must be at most 2000 characters")
    private String comment;

    private Long reviewImageId;

    /** Admin-only in effect; ignored when the caller isn't an admin. */
    private Boolean isVerifiedPurchase;
}
