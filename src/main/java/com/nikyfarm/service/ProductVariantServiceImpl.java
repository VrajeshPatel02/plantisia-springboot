package com.nikyfarm.service;

import com.nikyfarm.dto.ProductVariantRequestDto;
import com.nikyfarm.dto.ProductVariantResponseDto;
import com.nikyfarm.entity.Product;
import com.nikyfarm.entity.ProductImage;
import com.nikyfarm.entity.ProductVariant;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.ImageUploadException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.ProductVariantMapper;
import com.nikyfarm.repository.ProductImageRepository;
import com.nikyfarm.repository.ProductRepository;
import com.nikyfarm.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public ProductVariantResponseDto createVariant(Long productId, ProductVariantRequestDto dto) {
        Product product = findProductOrThrow(productId);
        assertSkuAvailable(dto.getSku(), null);
        assertValidPricing(dto.getPrice(), dto.getDiscountedPrice());

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        applyDto(variant, dto);

        ProductVariant saved = productVariantRepository.save(variant);
        if (Boolean.TRUE.equals(saved.getIsDefault())) {
            productVariantRepository.clearDefaultForProductExcept(productId, saved.getId());
        }
        return ProductVariantMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public ProductVariantResponseDto updateVariant(Long productId, Long variantId, ProductVariantRequestDto dto) {
        ProductVariant variant = findVariantOrThrow(productId, variantId);
        assertSkuAvailable(dto.getSku(), variantId);
        assertValidPricing(dto.getPrice(), dto.getDiscountedPrice());

        applyDto(variant, dto);
        ProductVariant saved = productVariantRepository.save(variant);
        if (Boolean.TRUE.equals(saved.getIsDefault())) {
            productVariantRepository.clearDefaultForProductExcept(productId, saved.getId());
        }
        return ProductVariantMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponseDto getVariantById(Long productId, Long variantId) {
        return ProductVariantMapper.toResponseDto(findVariantOrThrow(productId, variantId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponseDto> getVariantsByProduct(Long productId) {
        findProductOrThrow(productId);
        return productVariantRepository.findByProductId(productId).stream()
                .map(ProductVariantMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteVariant(Long productId, Long variantId) {
        ProductVariant variant = findVariantOrThrow(productId, variantId);

        List<ProductImage> variantImages = productImageRepository.findByVariantId(variantId);
        for (ProductImage image : variantImages) {
            if (image.getPublicId() != null) {
                try {
                    cloudinaryService.deleteImage(image.getPublicId());
                } catch (ImageUploadException e) {
                    System.err.println("Warning: failed to delete Cloudinary image (" + image.getPublicId() + "): " + e.getMessage());
                }
            }
        }
        productImageRepository.deleteAll(variantImages);
        productVariantRepository.delete(variant);
    }

    private void applyDto(ProductVariant variant, ProductVariantRequestDto dto) {
        variant.setVariantLabel(dto.getVariantLabel());
        variant.setSize(dto.getSize());
        variant.setColour(dto.getColour());
        variant.setPrice(dto.getPrice());
        variant.setDiscountedPrice(dto.getDiscountedPrice());
        variant.setStockQuantity(dto.getStockQuantity());
        variant.setSku(dto.getSku());
        variant.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : Boolean.FALSE);
        variant.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : Boolean.TRUE);
    }

    private void assertValidPricing(Double price, Double discountedPrice) {
        if (discountedPrice != null && price != null && discountedPrice > price) {
            throw new BusinessRuleViolationException("Discounted price cannot be greater than price");
        }
    }

    private void assertSkuAvailable(String sku, Long excludeId) {
        boolean taken = excludeId == null
                ? productVariantRepository.existsBySku(sku)
                : productVariantRepository.existsBySkuAndIdNot(sku, excludeId);
        if (taken) {
            throw new DuplicateResourceException("A variant with SKU '" + sku + "' already exists");
        }
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    private ProductVariant findVariantOrThrow(Long productId, Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));
        if (variant.getProduct() == null || !variant.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException("Variant " + variantId + " does not belong to product " + productId);
        }
        return variant;
    }
}
