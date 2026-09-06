package com.nikyfarm.service;

import com.nikyfarm.dto.ReviewImageResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReviewImageService {

    ReviewImageResponseDto upload(MultipartFile file);

    ReviewImageResponseDto getById(Long id);

    List<ReviewImageResponseDto> getAll();

    void delete(Long id);
}
