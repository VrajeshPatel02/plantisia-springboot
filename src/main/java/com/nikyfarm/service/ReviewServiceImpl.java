package com.nikyfarm.service;

import com.nikyfarm.dto.ReviewRequestDto;
import com.nikyfarm.dto.ReviewResponseDto;
import com.nikyfarm.dto.ReviewUpdateRequestDto;
import com.nikyfarm.entity.Product;
import com.nikyfarm.entity.ReviewImage;
import com.nikyfarm.entity.Reviews;
import com.nikyfarm.entity.User;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.ReviewMapper;
import com.nikyfarm.repository.ProductRepository;
import com.nikyfarm.repository.ReviewImageRepository;
import com.nikyfarm.repository.ReviewsRepository;
import com.nikyfarm.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewsRepository reviewsRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewImageService reviewImageService;

    @Override
    @Transactional
    public ReviewResponseDto createReview(Long userId, ReviewRequestDto dto) {
        Product product = findProductOrThrow(dto.getProductId());
        User user = findUserOrThrow(userId);

        if (reviewsRepository.existsByProductIdAndUserId(dto.getProductId(), userId)) {
            throw new DuplicateResourceException("You have already reviewed this product");
        }

        Reviews review = new Reviews();
        review.setProduct(product);
        review.setUser(user);
        review.setIsVerifiedPurchase(false); // no order-tracking service to verify against yet
        applyContent(review, dto.getRating(), dto.getTitle(), dto.getComment());
        if (dto.getReviewImageId() != null) {
            review.setReviewImage(findReviewImageOrThrow(dto.getReviewImageId()));
        }

        return ReviewMapper.toResponseDto(reviewsRepository.save(review));
    }

    @Override
    @Transactional
    public ReviewResponseDto updateReview(
            Long reviewId, Long currentUserId, boolean isAdmin, ReviewUpdateRequestDto dto) {
        Reviews review = findReviewOwnedOrThrow(reviewId, currentUserId, isAdmin);
        applyContent(review, dto.getRating(), dto.getTitle(), dto.getComment());
        if (dto.getReviewImageId() != null) {
            review.setReviewImage(findReviewImageOrThrow(dto.getReviewImageId()));
        }
        if (isAdmin && dto.getIsVerifiedPurchase() != null) {
            review.setIsVerifiedPurchase(dto.getIsVerifiedPurchase());
        }
        return ReviewMapper.toResponseDto(reviewsRepository.save(review));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsByProduct(Long productId) {
        findProductOrThrow(productId);
        return reviewsRepository.findByProductId(productId).stream()
                .map(ReviewMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getAllReviews() {
        return reviewsRepository.findAll().stream()
                .map(ReviewMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponseDto getById(Long reviewId) {
        return ReviewMapper.toResponseDto(reviewsRepository
                .findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId)));
    }

    @Override
    @Transactional
    public void removeImage(Long reviewId) {
        Reviews review = reviewsRepository
                .findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        if (review.getReviewImage() == null) {
            return;
        }
        Long reviewImageId = review.getReviewImage().getId();
        review.setReviewImage(null);
        reviewsRepository.save(review);
        reviewImageService.delete(reviewImageId);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, Long currentUserId, boolean isAdmin) {
        Reviews review = findReviewOwnedOrThrow(reviewId, currentUserId, isAdmin);
        Long reviewImageId =
                review.getReviewImage() != null ? review.getReviewImage().getId() : null;

        reviewsRepository.delete(review);

        if (reviewImageId != null) {
            reviewImageService.delete(reviewImageId);
        }
    }

    private void applyContent(Reviews review, Short rating, String title, String comment) {
        review.setRating(rating);
        review.setTitle(title);
        review.setComment(comment);
    }

    private Reviews findReviewOwnedOrThrow(Long reviewId, Long currentUserId, boolean isAdmin) {
        Reviews review = reviewsRepository
                .findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        boolean isOwner = review.getUser() != null && review.getUser().getId().equals(currentUserId);
        if (!isOwner && !isAdmin) {
            // 404 rather than 403 -- don't confirm to a caller that a review id they don't own exists.
            throw new ResourceNotFoundException("Review not found with id: " + reviewId);
        }
        return review;
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private ReviewImage findReviewImageOrThrow(Long id) {
        return reviewImageRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review image not found with id: " + id));
    }
}
