package com.nikyfarm.service;

import com.nikyfarm.dto.ImageUploadResult;
import com.nikyfarm.dto.ReviewImageResponseDto;
import com.nikyfarm.entity.Order;
import com.nikyfarm.entity.Product;
import com.nikyfarm.entity.ReviewImage;
import com.nikyfarm.entity.User;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.ReviewImageMapper;
import com.nikyfarm.repository.OrderItemsRepository;
import com.nikyfarm.repository.ProductRepository;
import com.nikyfarm.repository.ReviewImageRepository;
import com.nikyfarm.repository.ReviewsRepository;
import com.nikyfarm.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ReviewImageServiceImpl implements ReviewImageService {

    private static final String IMAGE_FOLDER = "reviews";

    private final ReviewImageRepository reviewImageRepository;
    private final ReviewsRepository reviewsRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public ReviewImageResponseDto upload(Long userId, Long productId, MultipartFile file) {
        if (userId == null) {
            throw new BusinessRuleViolationException(
                    "Authentication is required to upload a review image");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + productId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));

        if (!hasPaidPurchase(userId, productId)) {
            throw new BusinessRuleViolationException(
                    "You can upload a review image only after purchasing this product");
        }

        if (reviewsRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new BusinessRuleViolationException(
                    "You have already reviewed this product");
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("Review image file is required");
        }

        ImageUploadResult result = cloudinaryService.uploadImage(file, IMAGE_FOLDER);

        ReviewImage image = new ReviewImage();
        image.setImageUrl(result.getImageUrl());
        image.setPublicId(result.getPublicId());
        image.setUser(user);
        image.setProduct(product);

        return ReviewImageMapper.toResponseDto(reviewImageRepository.save(image));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewImageResponseDto getById(Long id) {
        return ReviewImageMapper.toResponseDto(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewImageResponseDto> getAll() {
        return reviewImageRepository.findAll().stream()
                .map(ReviewImageMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ReviewImage image = findOrThrow(id);

        if (reviewsRepository.existsByReviewImageId(id)) {
            throw new BusinessRuleViolationException(
                    "Cannot delete this image because a review still references it");
        }

        if (image.getPublicId() != null) {
            cloudinaryService.deleteImage(image.getPublicId());
        }

        reviewImageRepository.delete(image);
    }

    private boolean hasPaidPurchase(Long userId, Long productId) {
        return orderItemsRepository.existsPaidPurchase(
                userId,
                productId,
                Order.PaymentStatus.PAID);
    }

    private ReviewImage findOrThrow(Long id) {
        return reviewImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review image not found with id: " + id));
    }
}
