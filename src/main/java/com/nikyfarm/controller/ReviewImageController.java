package com.nikyfarm.controller;

import com.nikyfarm.dto.ReviewImageResponseDto;
import com.nikyfarm.service.ReviewImageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/review-images")
@RequiredArgsConstructor
public class ReviewImageController {

    private final ReviewImageService reviewImageService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ReviewImageResponseDto> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewImageService.upload(file));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewImageResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reviewImageService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ReviewImageResponseDto>> getAll() {
        return ResponseEntity.ok(reviewImageService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reviewImageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
