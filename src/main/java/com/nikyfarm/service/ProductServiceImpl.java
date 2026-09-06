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
        assertSlugAvailable(dto.getSlug(), null);
        assertValidPricing(dto.getBasePrice(), dto.getDiscountedPrice());

        Product product = new Product();
        applyDto(product, dto);
        return ProductMapper.toResponseDto(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponseDto updateProduct(Long id, ProductRequestDto dto) {
        Product product = findProductOrThrow(id);
        assertSlugAvailable(dto.getSlug(), id);
        assertValidPricing(dto.getBasePrice(), dto.getDiscountedPrice());

        applyDto(product, dto);
        return ProductMapper.toResponseDto(productRepository.save(product));
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
                System.err.println("Warning: failed to delete Cloudinary image (" + image.getPublicId() + "): " + e.getMessage());
            }
        }

        // ProductAttributeValue isn't mapped as a collection on Product (no cascade path),
        // so its rows must be removed explicitly or the FK constraint blocks the delete below.
        productAttributeValueRepository.deleteAll(productAttributeValueRepository.findByProductId(id));

        // cascade = ALL on Product's collections removes product_images and product_variants rows.
        productRepository.delete(product);
    }

    private void applyDto(Product product, ProductRequestDto dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + dto.getCategoryId()));

        product.setName(dto.getName());
        product.setSlug(dto.getSlug());
        product.setShortDescription(dto.getShortDescription());
        product.setDescription(dto.getDescription());
        product.setBasePrice(dto.getBasePrice());
        product.setDiscountedPrice(dto.getDiscountedPrice());
        product.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : Boolean.FALSE);
        product.setCategory(category);
    }

    private void assertValidPricing(Double basePrice, Double discountedPrice) {
        if (discountedPrice != null && basePrice != null && discountedPrice > basePrice) {
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

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }
}
