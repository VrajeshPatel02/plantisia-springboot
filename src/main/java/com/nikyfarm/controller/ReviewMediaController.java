package com.nikyfarm.controller;

import com.nikyfarm.dto.ReviewMediaResponseDto;
import com.nikyfarm.service.ReviewMediaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewMediaController {

    private final ReviewMediaService reviewMediaService;

    @PostMapping(value = "/{reviewId}/media", consumes = "multipart/form-data")
    public ResponseEntity<List<ReviewMediaResponseDto>> uploadMedia(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Long userId,
            Authentication authentication,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "video", required = false) MultipartFile video) {

        List<ReviewMediaResponseDto> media = reviewMediaService.uploadMedia(
                reviewId,
                userId,
                isAdmin(authentication),
                images,
                video);

        return ResponseEntity.status(HttpStatus.CREATED).body(media);
    }

    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<Void> deleteMedia(
            @PathVariable Long mediaId,
            @AuthenticationPrincipal Long userId,
            Authentication authentication) {

        reviewMediaService.deleteMedia(mediaId, userId, isAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
