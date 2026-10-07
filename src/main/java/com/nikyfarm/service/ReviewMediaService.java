package com.nikyfarm.service;

import com.nikyfarm.dto.ReviewMediaResponseDto;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ReviewMediaService {

    List<ReviewMediaResponseDto> uploadMedia(
            Long reviewId,
            Long currentUserId,
            boolean isAdmin,
            List<MultipartFile> images,
            MultipartFile video);

    void deleteMedia(Long mediaId, Long currentUserId, boolean isAdmin);
}
