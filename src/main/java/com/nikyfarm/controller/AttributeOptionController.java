package com.nikyfarm.controller;

import com.nikyfarm.dto.AttributeOptionRequestDto;
import com.nikyfarm.dto.AttributeOptionResponseDto;
import com.nikyfarm.service.AttributeOptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attributes/{attributeId}/options")
@RequiredArgsConstructor
public class AttributeOptionController {

    private final AttributeOptionService attributeOptionService;

    @PostMapping
    public ResponseEntity<AttributeOptionResponseDto> createOption(
            @PathVariable Long attributeId,
            @RequestBody @Valid AttributeOptionRequestDto request) {
        AttributeOptionResponseDto response = attributeOptionService.createOption(attributeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{optionId}")
    public ResponseEntity<AttributeOptionResponseDto> updateOption(
            @PathVariable Long attributeId,
            @PathVariable Long optionId,
            @RequestBody @Valid AttributeOptionRequestDto request) {
        return ResponseEntity.ok(attributeOptionService.updateOption(attributeId, optionId, request));
    }

    @GetMapping("/{optionId}")
    public ResponseEntity<AttributeOptionResponseDto> getOption(
            @PathVariable Long attributeId,
            @PathVariable Long optionId) {
        return ResponseEntity.ok(attributeOptionService.getOptionById(attributeId, optionId));
    }

    @GetMapping
    public ResponseEntity<List<AttributeOptionResponseDto>> getOptions(@PathVariable Long attributeId) {
        return ResponseEntity.ok(attributeOptionService.getOptionsByAttribute(attributeId));
    }

    @DeleteMapping("/{optionId}")
    public ResponseEntity<Void> deleteOption(
            @PathVariable Long attributeId,
            @PathVariable Long optionId) {
        attributeOptionService.deleteOption(attributeId, optionId);
        return ResponseEntity.noContent().build();
    }
}
