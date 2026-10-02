package com.nikyfarm.controller;

import com.nikyfarm.dto.ReviewEligibilityResponseDto;
import com.nikyfarm.dto.ReviewRequestDto;
import com.nikyfarm.dto.ReviewResponseDto;
import com.nikyfarm.dto.ReviewUpdateRequestDto;
import com.nikyfarm.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * No class-level @RequestMapping — reads/creates are nested under a product
 * ("/api/products/{productId}/reviews"), but a review has its own global id for update/delete
 * ("/api/reviews/{id}"), so each method spells out its full path.
 */
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/api/products/{productId}/reviews")
    public ResponseEntity<List<ReviewResponseDto>> getReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    @GetMapping("/api/products/{productId}/reviews/eligibility")
    public ResponseEntity<ReviewEligibilityResponseDto> getReviewEligibility(
            @PathVariable Long productId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(reviewService.getReviewEligibility(userId, productId));
    }

    @PostMapping("/api/products/{productId}/reviews")
    public ResponseEntity<ReviewResponseDto> createReview(
            @PathVariable Long productId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid ReviewRequestDto request) {
        request.setProductId(productId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(userId, request));
    }

    @PutMapping("/api/reviews/{id}")
    public ResponseEntity<ReviewResponseDto> updateReview(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            Authentication authentication,
            @RequestBody @Valid ReviewUpdateRequestDto request) {
        return ResponseEntity.ok(reviewService.updateReview(
                id, userId, isAdmin(authentication), request));
    }

    @DeleteMapping("/api/reviews/{id}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            Authentication authentication) {
        reviewService.deleteReview(id, userId, isAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
