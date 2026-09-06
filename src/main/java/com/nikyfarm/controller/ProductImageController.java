package com.nikyfarm.controller;

import com.nikyfarm.dto.BulkImageUploadResponse;
import com.nikyfarm.dto.ImageUploadResponse;
import com.nikyfarm.dto.ProductImageResponseDto;
import com.nikyfarm.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/images")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService productImageService;

    /** GET /api/products/1/images — every image for the product (general + variant-scoped). */
    @GetMapping
    public ResponseEntity<List<ProductImageResponseDto>> getImages(@PathVariable Long productId) {
        return ResponseEntity.ok(productImageService.getImagesByProduct(productId));
    }

    /** Single image. POST /api/products/1/images  (form-data key: "file") */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ImageUploadResponse> uploadProductImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) {
        ImageUploadResponse response = productImageService.uploadProductImage(productId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Bulk images in ONE request — use this for "add product" forms with multiple photos.
     * POST /api/products/1/images/bulk
     * form-data -> key "files" (type: File) -> select multiple files
     */
    @PostMapping(value = "/bulk", consumes = "multipart/form-data")
    public ResponseEntity<BulkImageUploadResponse> uploadProductImagesBulk(
            @PathVariable Long productId,
            @RequestParam("files") List<MultipartFile> files) {
        BulkImageUploadResponse response = productImageService.uploadProductImagesBulk(productId, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Single image. POST /api/products/1/images/variant/5  (form-data key: "file") */
    @PostMapping(value = "/variant/{variantId}", consumes = "multipart/form-data")
    public ResponseEntity<ImageUploadResponse> uploadVariantImage(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @RequestParam("file") MultipartFile file) {
        ImageUploadResponse response = productImageService.uploadVariantImage(productId, variantId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Bulk images for one variant. POST /api/products/1/images/variant/5/bulk (form-data key: "files") */
    @PostMapping(value = "/variant/{variantId}/bulk", consumes = "multipart/form-data")
    public ResponseEntity<BulkImageUploadResponse> uploadVariantImagesBulk(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @RequestParam("files") List<MultipartFile> files) {
        BulkImageUploadResponse response = productImageService.uploadVariantImagesBulk(productId, variantId, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Replaces the actual image file behind an existing ProductImage row:
     * deletes the old Cloudinary file, uploads the new one, updates the same DB row
     * (id, product, variant, sort_order, is_primary all stay unchanged — only the file changes).
     *
     * PUT /api/products/1/images/42   (form-data key: "file")
     */
    @PutMapping(value = "/{imageId}", consumes = "multipart/form-data")
    public ResponseEntity<ImageUploadResponse> replaceProductImage(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(productImageService.replaceProductImage(productId, imageId, file));
    }

    /** DELETE /api/products/1/images/42 — removes from Cloudinary AND the DB row. */
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteProductImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        productImageService.deleteProductImage(productId, imageId);
        return ResponseEntity.noContent().build();
    }
}
