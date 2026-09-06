package com.nikyfarm.controller;

import com.nikyfarm.dto.ProductAttributeValueRequestDto;
import com.nikyfarm.dto.ProductAttributeValueResponseDto;
import com.nikyfarm.service.ProductAttributeValueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/attribute-values")
@RequiredArgsConstructor
public class ProductAttributeValueController {

    private final ProductAttributeValueService productAttributeValueService;

    @PostMapping
    public ResponseEntity<ProductAttributeValueResponseDto> createValue(
            @PathVariable Long productId,
            @RequestBody @Valid ProductAttributeValueRequestDto request) {
        ProductAttributeValueResponseDto response = productAttributeValueService.createValue(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{valueId}")
    public ResponseEntity<ProductAttributeValueResponseDto> updateValue(
            @PathVariable Long productId,
            @PathVariable Long valueId,
            @RequestBody @Valid ProductAttributeValueRequestDto request) {
        return ResponseEntity.ok(productAttributeValueService.updateValue(productId, valueId, request));
    }

    @GetMapping("/{valueId}")
    public ResponseEntity<ProductAttributeValueResponseDto> getValue(
            @PathVariable Long productId,
            @PathVariable Long valueId) {
        return ResponseEntity.ok(productAttributeValueService.getValueById(productId, valueId));
    }

    @GetMapping
    public ResponseEntity<List<ProductAttributeValueResponseDto>> getValues(@PathVariable Long productId) {
        return ResponseEntity.ok(productAttributeValueService.getValuesByProduct(productId));
    }

    @DeleteMapping("/{valueId}")
    public ResponseEntity<Void> deleteValue(
            @PathVariable Long productId,
            @PathVariable Long valueId) {
        productAttributeValueService.deleteValue(productId, valueId);
        return ResponseEntity.noContent().build();
    }
}
