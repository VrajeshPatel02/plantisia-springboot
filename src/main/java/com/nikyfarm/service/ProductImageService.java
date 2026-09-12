package com.nikyfarm.service;

import com.nikyfarm.dto.BulkImageUploadResponse;
import com.nikyfarm.dto.ImageUploadResponse;
import com.nikyfarm.dto.ProductImageResponseDto;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ProductImageService {

    ImageUploadResponse uploadProductImage(Long productId, MultipartFile file);

    BulkImageUploadResponse uploadProductImagesBulk(Long productId, List<MultipartFile> files);

    ImageUploadResponse uploadVariantImage(Long productId, Long variantId, MultipartFile file);

    BulkImageUploadResponse uploadVariantImagesBulk(Long productId, Long variantId, List<MultipartFile> files);

    ImageUploadResponse replaceProductImage(Long productId, Long imageId, MultipartFile file);

    void deleteProductImage(Long productId, Long imageId);

    List<ProductImageResponseDto> getImagesByProduct(Long productId);

    /** Product-level images only (not scoped to any variant). */
    List<ProductImageResponseDto> getGeneralImages(Long productId);

    /** Images scoped to one specific variant. */
    List<ProductImageResponseDto> getImagesByVariant(Long productId, Long variantId);
}
