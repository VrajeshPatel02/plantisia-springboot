package com.nikyfarm.controller;

import com.nikyfarm.dto.AttributeRequestDto;
import com.nikyfarm.dto.AttributeResponseDto;
import com.nikyfarm.service.AttributeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attributes")
@RequiredArgsConstructor
public class AttributeController {

    private final AttributeService attributeService;

    @PostMapping
    public ResponseEntity<AttributeResponseDto> createAttribute(@RequestBody @Valid AttributeRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attributeService.createAttribute(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttributeResponseDto> updateAttribute(
            @PathVariable Long id, @RequestBody @Valid AttributeRequestDto request) {
        return ResponseEntity.ok(attributeService.updateAttribute(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttributeResponseDto> getAttribute(@PathVariable Long id) {
        return ResponseEntity.ok(attributeService.getAttributeById(id));
    }

    @GetMapping
    public ResponseEntity<List<AttributeResponseDto>> getAttributes(@RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(attributeService.getAttributes(categoryId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttribute(@PathVariable Long id) {
        attributeService.deleteAttribute(id);
        return ResponseEntity.noContent().build();
    }
}
