package com.nikyfarm.service;

import com.nikyfarm.dto.BulkImageUploadResponse;
import com.nikyfarm.dto.ImageUploadResponse;
import com.nikyfarm.dto.ImageUploadResult;
import com.nikyfarm.dto.ProductImageResponseDto;
import com.nikyfarm.entity.Product;
import com.nikyfarm.entity.ProductImage;
import com.nikyfarm.entity.ProductVariant;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.ProductImageMapper;
import com.nikyfarm.repository.ProductImageRepository;
import com.nikyfarm.repository.ProductRepository;
import com.nikyfarm.repository.ProductVariantRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl implements ProductImageService {

    private final CloudinaryService cloudinaryService;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional
    public ImageUploadResponse uploadProductImage(Long productId, MultipartFile file) {
        Product product = findProductOrThrow(productId);
        ImageUploadResult result = cloudinaryService.uploadImage(file, "products/" + productId);
        ProductImage saved = saveProductImage(product, null, result.getImageUrl(), result.getPublicId());
        return new ImageUploadResponse(saved.getId(), result.getImageUrl(), "Product image uploaded successfully.");
    }

    @Override
    @Transactional
    public BulkImageUploadResponse uploadProductImagesBulk(Long productId, List<MultipartFile> files) {
        Product product = findProductOrThrow(productId);
        List<ImageUploadResult> results = cloudinaryService.uploadImages(files, "products/" + productId);
        for (ImageUploadResult result : results) {
            if (result.isSuccess()) {
                saveProductImage(product, null, result.getImageUrl(), result.getPublicId());
            }
        }
        return buildBulkResponse(results);
    }

    @Override
    @Transactional
    public ImageUploadResponse uploadVariantImage(Long productId, Long variantId, MultipartFile file) {
        Product product = findProductOrThrow(productId);
        ProductVariant variant = findVariantOrThrow(productId, variantId, product);
        ImageUploadResult result =
                cloudinaryService.uploadImage(file, "products/" + productId + "/variants/" + variantId);
        ProductImage saved = saveProductImage(product, variant, result.getImageUrl(), result.getPublicId());
        return new ImageUploadResponse(saved.getId(), result.getImageUrl(), "Variant image uploaded successfully.");
    }

    @Override
    @Transactional
    public BulkImageUploadResponse uploadVariantImagesBulk(Long productId, Long variantId, List<MultipartFile> files) {
        Product product = findProductOrThrow(productId);
        ProductVariant variant = findVariantOrThrow(productId, variantId, product);
        List<ImageUploadResult> results =
                cloudinaryService.uploadImages(files, "products/" + productId + "/variants/" + variantId);
        for (ImageUploadResult result : results) {
            if (result.isSuccess()) {
                saveProductImage(product, variant, result.getImageUrl(), result.getPublicId());
            }
        }
        return buildBulkResponse(results);
    }

    @Override
    @Transactional
    public ImageUploadResponse replaceProductImage(Long productId, Long imageId, MultipartFile file) {
        ProductImage existing = findImageOrThrow(productId, imageId);
        String folder = existing.getVariant() != null
                ? "products/" + productId + "/variants/" + existing.getVariant().getId()
                : "products/" + productId;

        ImageUploadResult result = cloudinaryService.replaceImage(existing.getPublicId(), file, folder);
        existing.setImageUrl(result.getImageUrl());
        existing.setPublicId(result.getPublicId());
        productImageRepository.save(existing);

        return new ImageUploadResponse(existing.getId(), result.getImageUrl(), "Image replaced successfully.");
    }

    @Override
    @Transactional
    public void deleteProductImage(Long productId, Long imageId) {
        ProductImage existing = findImageOrThrow(productId, imageId);
        cloudinaryService.deleteImage(existing.getPublicId());
        productImageRepository.delete(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductImageResponseDto> getImagesByProduct(Long productId) {
        findProductOrThrow(productId);
        return productImageRepository.findByProductId(productId).stream()
                .map(ProductImageMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductImageResponseDto> getGeneralImages(Long productId) {
        findProductOrThrow(productId);
        return productImageRepository.findByProductIdAndVariantIsNull(productId).stream()
                .map(ProductImageMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductImageResponseDto> getImagesByVariant(Long productId, Long variantId) {
        Product product = findProductOrThrow(productId);
        findVariantOrThrow(productId, variantId, product);
        return productImageRepository.findByVariantId(variantId).stream()
                .map(ProductImageMapper::toResponseDto)
                .toList();
    }

    private ProductImage saveProductImage(Product product, ProductVariant variant, String imageUrl, String publicId) {
        ProductImage productImage = new ProductImage();
        productImage.setProduct(product);
        productImage.setVariant(variant); // null for product-level images
        productImage.setImageUrl(imageUrl);
        productImage.setPublicId(publicId);
        productImage.setIsPrimary(variant == null && productImageRepository.countByProductId(product.getId()) == 0);
        return productImageRepository.save(productImage);
    }

    private BulkImageUploadResponse buildBulkResponse(List<ImageUploadResult> results) {
        int successCount =
                (int) results.stream().filter(ImageUploadResult::isSuccess).count();
        int failureCount = results.size() - successCount;
        return new BulkImageUploadResponse(successCount, failureCount, results);
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    private ProductVariant findVariantOrThrow(Long productId, Long variantId, Product product) {
        ProductVariant variant = productVariantRepository
                .findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));
        if (!variant.getProduct().getId().equals(product.getId())) {
            throw new ResourceNotFoundException("Variant " + variantId + " does not belong to product " + productId);
        }
        return variant;
    }

    private ProductImage findImageOrThrow(Long productId, Long imageId) {
        ProductImage image = productImageRepository
                .findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));
        if (!image.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException("Image " + imageId + " does not belong to product " + productId);
        }
        return image;
    }
}
