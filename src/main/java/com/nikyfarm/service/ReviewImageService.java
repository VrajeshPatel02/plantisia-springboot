package com.nikyfarm.service;

import com.nikyfarm.dto.ReviewImageResponseDto;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ReviewImageService {

    ReviewImageResponseDto upload(Long userId, Long productId, MultipartFile file);

    ReviewImageResponseDto getById(Long id);

    List<ReviewImageResponseDto> getAll();

    void delete(Long id);
}
