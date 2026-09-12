package com.nikyfarm.service;

import com.nikyfarm.dto.ProductRequestDto;
import com.nikyfarm.dto.ProductResponseDto;
import com.nikyfarm.entity.Category;
import com.nikyfarm.entity.Product;
import com.nikyfarm.entity.ProductImage;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.ImageUploadException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.ProductMapper;
import com.nikyfarm.repository.CategoryRepository;
import com.nikyfarm.repository.ProductAttributeValueRepository;
import com.nikyfarm.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto dto) {

        String normalizedSlug = normalizeSlug(dto.getSlug());

        assertSlugAvailable(normalizedSlug, null);

        assertValidPricing(dto.getBasePrice(), dto.getDiscountedPrice());

        Product product = new Product();

        applyDto(product, dto, normalizedSlug);

        Product saved = productRepository.save(product);

        return ProductMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public ProductResponseDto updateProduct(Long id, ProductRequestDto dto) {

        Product product = findProductOrThrow(id);

        String normalizedSlug = normalizeSlug(dto.getSlug());

        assertSlugAvailable(normalizedSlug, id);

        assertValidPricing(dto.getBasePrice(), dto.getDiscountedPrice());

        applyDto(product, dto, normalizedSlug);

        Product saved = productRepository.save(product);

        return ProductMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id) {

        return ProductMapper.toResponseDto(findProductOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getProducts(Long categoryId, Pageable pageable) {

        Page<Product> page = categoryId != null
                ? productRepository.findByCategoryId(categoryId, pageable)
                : productRepository.findAll(pageable);

        return page.map(ProductMapper::toResponseDto);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {

        Product product = findProductOrThrow(id);

        for (ProductImage image : product.getProductImages()) {

            if (image.getPublicId() == null) {
                continue;
            }

            try {

                cloudinaryService.deleteImage(image.getPublicId());

            } catch (ImageUploadException e) {

                System.err.println(
                        "Warning: failed to delete Cloudinary image (" + image.getPublicId() + "): " + e.getMessage());
            }
        }

        productAttributeValueRepository.deleteAll(productAttributeValueRepository.findByProductId(id));

        productRepository.delete(product);
    }

    private void applyDto(Product product, ProductRequestDto dto, String normalizedSlug) {

        Category category = categoryRepository
                .findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + dto.getCategoryId()));

        product.setName(dto.getName().trim());

        product.setSlug(normalizedSlug);

        product.setShortDescription(trimToNull(dto.getShortDescription()));

        product.setDescription(trimToNull(dto.getDescription()));

        product.setBasePrice(dto.getBasePrice());

        product.setDiscountedPrice(dto.getDiscountedPrice());

        product.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : Boolean.TRUE);

        product.setCategory(category);
    }

    private void assertValidPricing(BigDecimal basePrice, BigDecimal discountedPrice) {

        if (basePrice == null) {

            throw new BusinessRuleViolationException("Base price is required");
        }

        if (basePrice.compareTo(BigDecimal.ZERO) < 0) {

            throw new BusinessRuleViolationException("Base price cannot be negative");
        }

        if (discountedPrice != null && discountedPrice.compareTo(BigDecimal.ZERO) < 0) {

            throw new BusinessRuleViolationException("Discounted price cannot be negative");
        }

        if (discountedPrice != null && discountedPrice.compareTo(basePrice) > 0) {

            throw new BusinessRuleViolationException("Discounted price cannot be greater than base price");
        }
    }

    private void assertSlugAvailable(String slug, Long excludeId) {

        boolean taken = excludeId == null
                ? productRepository.existsBySlug(slug)
                : productRepository.existsBySlugAndIdNot(slug, excludeId);

        if (taken) {

            throw new DuplicateResourceException("A product with slug '" + slug + "' already exists");
        }
    }

    private String normalizeSlug(String slug) {

        if (slug == null || slug.isBlank()) {

            throw new BusinessRuleViolationException("Slug is required");
        }

        String normalized = slug.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (normalized.isBlank()) {

            throw new BusinessRuleViolationException("Slug must contain valid characters");
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

    private Product findProductOrThrow(Long id) {

        return productRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }
}
