package com.nikyfarm.controller;

import com.nikyfarm.dto.ProductVariantRequestDto;
import com.nikyfarm.dto.ProductVariantResponseDto;
import com.nikyfarm.service.ProductVariantService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/{productId}/variants")
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    @PostMapping
    public ResponseEntity<ProductVariantResponseDto> createVariant(
            @PathVariable Long productId, @RequestBody @Valid ProductVariantRequestDto request) {

        ProductVariantResponseDto response = productVariantService.createVariant(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{variantId}")
    public ResponseEntity<ProductVariantResponseDto> updateVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @RequestBody @Valid ProductVariantRequestDto request) {

        return ResponseEntity.ok(productVariantService.updateVariant(productId, variantId, request));
    }

    @GetMapping("/{variantId}")
    public ResponseEntity<ProductVariantResponseDto> getVariant(
            @PathVariable Long productId, @PathVariable Long variantId) {

        return ResponseEntity.ok(productVariantService.getVariantById(productId, variantId));
    }

    @GetMapping
    public ResponseEntity<List<ProductVariantResponseDto>> getVariants(@PathVariable Long productId) {
        return ResponseEntity.ok(productVariantService.getVariantsByProduct(productId));
    }

    @DeleteMapping("/{variantId}")
    public ResponseEntity<Void> deleteVariant(@PathVariable Long productId, @PathVariable Long variantId) {

        productVariantService.deleteVariant(productId, variantId);
        return ResponseEntity.noContent().build();
    }
}
