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
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        String normalizedSku = normalizeSku(dto.getSku());

        assertSkuAvailable(normalizedSku, null);

        assertValidPricing(dto.getPrice(), dto.getDiscountedPrice());

        assertValidStock(dto.getStockQuantity());

        assertDefaultVariantState(dto);

        ProductVariant variant = new ProductVariant();

        variant.setProduct(product);

        applyDto(variant, dto, normalizedSku);

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

        String normalizedSku = normalizeSku(dto.getSku());

        assertSkuAvailable(normalizedSku, variantId);

        assertValidPricing(dto.getPrice(), dto.getDiscountedPrice());

        assertValidStock(dto.getStockQuantity());

        assertDefaultVariantState(dto);

        boolean wasDefault = Boolean.TRUE.equals(variant.getIsDefault());

        applyDto(variant, dto, normalizedSku);

        ProductVariant saved = productVariantRepository.save(variant);

        if (Boolean.TRUE.equals(saved.getIsDefault())) {

            productVariantRepository.clearDefaultForProductExcept(productId, saved.getId());

        } else if (wasDefault && !Boolean.TRUE.equals(saved.getIsActive())) {

            assignNewDefaultVariant(productId);
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

        boolean wasDefault = Boolean.TRUE.equals(variant.getIsDefault());

        List<ProductImage> variantImages = productImageRepository.findByVariantId(variantId);

        for (ProductImage image : variantImages) {

            if (image.getPublicId() != null) {

                try {

                    cloudinaryService.deleteImage(image.getPublicId());

                } catch (ImageUploadException e) {

                    System.err.println("Warning: failed to delete Cloudinary image ("
                            + image.getPublicId()
                            + "): "
                            + e.getMessage());
                }
            }
        }

        productImageRepository.deleteAll(variantImages);

        productVariantRepository.delete(variant);

        productVariantRepository.flush();

        if (wasDefault) {

            assignNewDefaultVariant(productId);
        }
    }

    private void applyDto(ProductVariant variant, ProductVariantRequestDto dto, String normalizedSku) {

        boolean isActive = dto.getIsActive() != null ? dto.getIsActive() : Boolean.TRUE;

        boolean isDefault = dto.getIsDefault() != null ? dto.getIsDefault() : Boolean.FALSE;

        variant.setVariantLabel(dto.getVariantLabel().trim());

        variant.setSize(trimToNull(dto.getSize()));

        variant.setColour(trimToNull(dto.getColour()));

        variant.setPrice(dto.getPrice());

        variant.setDiscountedPrice(dto.getDiscountedPrice());

        variant.setStockQuantity(dto.getStockQuantity());

        variant.setSku(normalizedSku);

        variant.setIsActive(isActive);

        variant.setIsDefault(isDefault);
    }

    private void assertValidPricing(BigDecimal price, BigDecimal discountedPrice) {

        if (price == null) {

            throw new BusinessRuleViolationException("Price is required");
        }

        if (price.compareTo(BigDecimal.ZERO) < 0) {

            throw new BusinessRuleViolationException("Price cannot be negative");
        }

        if (discountedPrice != null && discountedPrice.compareTo(BigDecimal.ZERO) < 0) {

            throw new BusinessRuleViolationException("Discounted price cannot be negative");
        }

        if (discountedPrice != null && discountedPrice.compareTo(price) > 0) {

            throw new BusinessRuleViolationException("Discounted price cannot be greater than price");
        }
    }

    private void assertValidStock(Integer stockQuantity) {

        if (stockQuantity == null) {

            throw new BusinessRuleViolationException("Stock quantity is required");
        }

        if (stockQuantity < 0) {

            throw new BusinessRuleViolationException("Stock quantity cannot be negative");
        }
    }

    private void assertDefaultVariantState(ProductVariantRequestDto dto) {

        boolean isDefault = Boolean.TRUE.equals(dto.getIsDefault());

        boolean isActive = dto.getIsActive() == null || dto.getIsActive();

        if (isDefault && !isActive) {

            throw new BusinessRuleViolationException("An inactive variant cannot be the default variant");
        }
    }

    private void assignNewDefaultVariant(Long productId) {

        Optional<ProductVariant> replacement =
                productVariantRepository.findFirstByProductIdAndIsActiveTrueOrderByIdAsc(productId);

        replacement.ifPresent(variant -> {
            productVariantRepository.clearDefaultForProduct(productId);

            variant.setIsDefault(true);

            productVariantRepository.save(variant);
        });
    }

    private void assertSkuAvailable(String sku, Long excludeId) {

        boolean taken = excludeId == null
                ? productVariantRepository.existsBySku(sku)
                : productVariantRepository.existsBySkuAndIdNot(sku, excludeId);

        if (taken) {

            throw new DuplicateResourceException("A variant with SKU '" + sku + "' already exists");
        }
    }

    private String normalizeSku(String sku) {

        if (sku == null || sku.isBlank()) {

            throw new BusinessRuleViolationException("SKU is required");
        }

        String normalized = sku.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "-");

        if (normalized.isBlank()) {

            throw new BusinessRuleViolationException("SKU must contain valid characters");
        }

        return normalized;
    }

    private String trimToNull(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private Product findProductOrThrow(Long productId) {

        return productRepository
                .findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    private ProductVariant findVariantOrThrow(Long productId, Long variantId) {

        ProductVariant variant = productVariantRepository
                .findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        if (variant.getProduct() == null || !variant.getProduct().getId().equals(productId)) {

            throw new ResourceNotFoundException("Variant " + variantId + " does not belong to product " + productId);
        }

        return variant;
    }
}
