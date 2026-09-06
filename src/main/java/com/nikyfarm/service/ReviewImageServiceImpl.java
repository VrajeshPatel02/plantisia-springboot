package com.nikyfarm.service;

import com.nikyfarm.dto.ImageUploadResult;
import com.nikyfarm.dto.ReviewImageResponseDto;
import com.nikyfarm.entity.ReviewImage;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.ReviewImageMapper;
import com.nikyfarm.repository.ReviewImageRepository;
import com.nikyfarm.repository.ReviewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewImageServiceImpl implements ReviewImageService {

    private static final String IMAGE_FOLDER = "reviews";

    private final ReviewImageRepository reviewImageRepository;
    private final ReviewsRepository reviewsRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public ReviewImageResponseDto upload(MultipartFile file) {
        ImageUploadResult result = cloudinaryService.uploadImage(file, IMAGE_FOLDER);
        ReviewImage image = new ReviewImage();
        image.setImageUrl(result.getImageUrl());
        image.setPublicId(result.getPublicId());
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
        return reviewImageRepository.findAll().stream().map(ReviewImageMapper::toResponseDto).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ReviewImage image = findOrThrow(id);
        if (reviewsRepository.existsByReviewImageId(id)) {
            throw new BusinessRuleViolationException("Cannot delete this image because a review still references it");
        }
        if (image.getPublicId() != null) {
            cloudinaryService.deleteImage(image.getPublicId());
        }
        reviewImageRepository.delete(image);
    }

    private ReviewImage findOrThrow(Long id) {
        return reviewImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review image not found with id: " + id));
    }
}
