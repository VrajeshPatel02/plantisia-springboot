package com.nikyfarm.service;

import com.nikyfarm.dto.ImageUploadResult;
import com.nikyfarm.dto.ReviewMediaResponseDto;
import com.nikyfarm.entity.ReviewMedia;
import com.nikyfarm.entity.ReviewMediaType;
import com.nikyfarm.entity.Reviews;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.repository.ReviewMediaRepository;
import com.nikyfarm.repository.ReviewsRepository;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ReviewMediaServiceImpl implements ReviewMediaService {

    private static final String MEDIA_FOLDER =
            "reviews/media";

    private static final int MAX_IMAGES_PER_REVIEW = 5;

    private static final int MAX_VIDEOS_PER_REVIEW = 1;

    private final ReviewMediaRepository reviewMediaRepository;

    private final ReviewsRepository reviewsRepository;

    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public List<ReviewMediaResponseDto> uploadMedia(
            Long reviewId,
            Long currentUserId,
            boolean isAdmin,
            List<MultipartFile> images,
            MultipartFile video) {

        Reviews review =
                findReviewOrThrow(reviewId);

        assertOwnerOrAdmin(
                review,
                currentUserId,
                isAdmin);

        List<MultipartFile> safeImages =
                images == null
                        ? List.of()
                        : images.stream()
                        .filter(file ->
                                file != null
                                        && !file.isEmpty())
                        .toList();

        boolean hasVideo =
                video != null
                        && !video.isEmpty();

        if (safeImages.isEmpty()
                && !hasVideo) {

            throw new BusinessRuleViolationException(
                    "At least one image or video is required.");
        }

        long existingImages =
                reviewMediaRepository
                        .countByReviewIdAndMediaType(
                                reviewId,
                                ReviewMediaType.IMAGE);

        long existingVideos =
                reviewMediaRepository
                        .countByReviewIdAndMediaType(
                                reviewId,
                                ReviewMediaType.VIDEO);

        if (existingImages
                + safeImages.size()
                > MAX_IMAGES_PER_REVIEW) {

            throw new BusinessRuleViolationException(
                    "A review can contain at most "
                            + MAX_IMAGES_PER_REVIEW
                            + " images.");
        }

        if (hasVideo
                && existingVideos + 1
                > MAX_VIDEOS_PER_REVIEW) {

            throw new BusinessRuleViolationException(
                    "A review can contain at most one video.");
        }

        List<ReviewMedia> savedMedia =
                new ArrayList<>();

        List<UploadedResource> uploadedResources =
                new ArrayList<>();

        try {

            int nextSortOrder =
                    getNextSortOrder(reviewId);

            // -------------------------------------------------------------
            // IMAGE UPLOADS
            // -------------------------------------------------------------

            for (MultipartFile image : safeImages) {

                ImageUploadResult result =
                        cloudinaryService.uploadImage(
                                image,
                                MEDIA_FOLDER);

                uploadedResources.add(
                        new UploadedResource(
                                ReviewMediaType.IMAGE,
                                result.getPublicId()));

                savedMedia.add(
                        buildMedia(
                                review,
                                ReviewMediaType.IMAGE,
                                result,
                                nextSortOrder++));

            }

            // -------------------------------------------------------------
            // VIDEO UPLOAD
            // -------------------------------------------------------------

            if (hasVideo) {

                ImageUploadResult result =
                        cloudinaryService.uploadVideo(
                                video,
                                MEDIA_FOLDER);

                uploadedResources.add(
                        new UploadedResource(
                                ReviewMediaType.VIDEO,
                                result.getPublicId()));

                savedMedia.add(
                        buildMedia(
                                review,
                                ReviewMediaType.VIDEO,
                                result,
                                nextSortOrder));
            }

            // -------------------------------------------------------------
            // DATABASE SAVE
            // -------------------------------------------------------------

            return reviewMediaRepository
                    .saveAll(savedMedia)
                    .stream()
                    .map(
                            ReviewMediaServiceImpl
                                    ::toResponseDto)
                    .toList();

        } catch (RuntimeException ex) {

            /*
             * If Cloudinary succeeds but DB persistence fails,
             * remove every resource uploaded during this request.
             */
            cleanupUploadedResources(
                    uploadedResources);

            throw ex;
        }
    }

    @Override
    @Transactional
    public void deleteMedia(
            Long mediaId,
            Long currentUserId,
            boolean isAdmin) {

        ReviewMedia media =
                reviewMediaRepository
                        .findById(mediaId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Review media not found with id: "
                                                        + mediaId));

        Reviews review =
                media.getReview();

        if (review == null) {

            throw new ResourceNotFoundException(
                    "Review not found for media: "
                            + mediaId);
        }

        assertOwnerOrAdmin(
                review,
                currentUserId,
                isAdmin);

        deleteFromCloudinary(media);

        reviewMediaRepository.delete(media);
    }

    private ReviewMedia buildMedia(
            Reviews review,
            ReviewMediaType type,
            ImageUploadResult result,
            int sortOrder) {

        ReviewMedia media =
                new ReviewMedia();

        media.setReview(review);

        media.setMediaType(type);

        media.setMediaUrl(
                result.getImageUrl());

        media.setPublicId(
                result.getPublicId());

        media.setThumbnailUrl(null);

        media.setSortOrder(sortOrder);

        return media;
    }

    private int getNextSortOrder(
            Long reviewId) {

        return reviewMediaRepository
                .findByReviewIdOrderBySortOrderAscIdAsc(
                        reviewId)
                .stream()
                .map(ReviewMedia::getSortOrder)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .map(value -> value + 1)
                .orElse(0);
    }

    private Reviews findReviewOrThrow(
            Long reviewId) {

        return reviewsRepository
                .findById(reviewId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Review not found with id: "
                                                + reviewId));
    }

    private void assertOwnerOrAdmin(
            Reviews review,
            Long currentUserId,
            boolean isAdmin) {

        boolean isOwner =
                review.getUser() != null
                        && review.getUser()
                        .getId()
                        .equals(currentUserId);

        if (!isOwner && !isAdmin) {

            throw new BusinessRuleViolationException(
                    "You are not allowed to modify media for this review.");
        }
    }

    private void deleteFromCloudinary(
            ReviewMedia media) {

        if (media.getPublicId() == null
                || media.getPublicId().isBlank()) {

            return;
        }

        if (media.getMediaType()
                == ReviewMediaType.VIDEO) {

            cloudinaryService.deleteVideo(
                    media.getPublicId());

        } else {

            cloudinaryService.deleteImage(
                    media.getPublicId());
        }
    }

    private void cleanupUploadedResources(
            List<UploadedResource> resources) {

        for (UploadedResource resource : resources) {

            try {

                if (resource.type()
                        == ReviewMediaType.VIDEO) {

                    cloudinaryService.deleteVideo(
                            resource.publicId());

                } else {

                    cloudinaryService.deleteImage(
                            resource.publicId());
                }

            } catch (RuntimeException cleanupException) {

                System.err.println(
                        "Warning: failed to clean up Cloudinary resource "
                                + resource.publicId()
                                + ": "
                                + cleanupException.getMessage());
            }
        }
    }

    private static ReviewMediaResponseDto toResponseDto(
            ReviewMedia media) {

        return new ReviewMediaResponseDto(
                media.getId(),
                media.getMediaType() != null
                        ? media.getMediaType().name()
                        : null,
                media.getMediaUrl(),
                media.getThumbnailUrl(),
                media.getSortOrder());
    }

    private record UploadedResource(
            ReviewMediaType type,
            String publicId) {
    }
}