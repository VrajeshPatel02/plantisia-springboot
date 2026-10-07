package com.nikyfarm.service;

import com.nikyfarm.dto.ProductRequestDto;
import com.nikyfarm.dto.ProductResponseDto;
import com.nikyfarm.entity.Category;
import com.nikyfarm.entity.Product;
import com.nikyfarm.entity.ProductImage;
import com.nikyfarm.entity.Reviews;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.ImageUploadException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.ProductMapper;
import com.nikyfarm.repository.CategoryRepository;
import com.nikyfarm.repository.ProductAttributeValueRepository;
import com.nikyfarm.repository.ProductRepository;
import com.nikyfarm.repository.ReviewsRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private final ReviewsRepository reviewsRepository;
    private final CloudinaryService cloudinaryService;

    // ============================================================
    // CREATE PRODUCT
    // ============================================================

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

    // ============================================================
    // UPDATE PRODUCT
    // ============================================================

    @Override
    @Transactional
    public ProductResponseDto updateProduct(
            Long id,
            ProductRequestDto dto
    ) {

        Product product = findProductOrThrow(id);

        String normalizedSlug = normalizeSlug(dto.getSlug());

        assertSlugAvailable(normalizedSlug, id);
        assertValidPricing(dto.getBasePrice(), dto.getDiscountedPrice());

        applyDto(product, dto, normalizedSlug);

        Product saved = productRepository.save(product);

        return ProductMapper.toResponseDto(saved);
    }

    // ============================================================
    // GET PRODUCT BY ID
    // ============================================================

    /**
     * Returns a single product.
     *
     * Review information is calculated from the Reviews table rather
     * than relying on duplicated avgRating/reviewCount values stored
     * on Product.
     */
    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id) {

        Product product = findProductOrThrow(id);

        List<Reviews> reviews =
                reviewsRepository.findByProductId(id);

        ReviewSummary summary =
                calculateReviewSummary(reviews);

        return ProductMapper.toResponseDto(
                product,
                summary.avgRating(),
                summary.reviewCount()
        );
    }

    // ============================================================
    // GET PRODUCTS - CATEGORY ONLY
    // ============================================================

    /**
     * Supports:
     *
     * GET /api/products?categoryId=1
     *
     * This method is required by ProductService.
     *
     * It delegates to the main 3-parameter implementation with
     * no search value.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getProducts(
            Long categoryId,
            Pageable pageable
    ) {
        return getProducts(
                categoryId,
                null,
                pageable
        );
    }

    // ============================================================
    // GET PRODUCTS - CATEGORY + SEARCH
    // ============================================================

    /**
     * Returns a paginated list of products.
     *
     * Review information is loaded using one grouped query for
     * the entire page to avoid an N+1 review-query problem.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getProducts(
            Long categoryId,
            String search,
            Pageable pageable
    ) {

        Page<Product> page;

        String normalizedSearch =
                search == null
                        ? null
                        : search.trim();

        // --------------------------------------------------------
        // No search
        // --------------------------------------------------------

        if (normalizedSearch == null
                || normalizedSearch.isBlank()) {

            page = categoryId != null
                    ? productRepository.findByCategoryId(
                    categoryId,
                    pageable
            )
                    : productRepository.findAll(pageable);

        }

        // --------------------------------------------------------
        // Search
        // --------------------------------------------------------

        else {

            page = productRepository.searchProducts(
                    normalizedSearch,
                    categoryId,
                    pageable
            );
        }

        // --------------------------------------------------------
        // Load review summaries for the complete page
        // --------------------------------------------------------

        List<Long> productIds = page.getContent()
                .stream()
                .map(Product::getId)
                .toList();

        Map<Long, ReviewSummary> reviewSummaries =
                loadReviewSummaries(productIds);

        // --------------------------------------------------------
        // Map products to response DTOs
        // --------------------------------------------------------

        return page.map(product -> {

            ReviewSummary summary =
                    reviewSummaries.getOrDefault(
                            product.getId(),
                            ReviewSummary.empty()
                    );

            return ProductMapper.toResponseDto(
                    product,
                    summary.avgRating(),
                    summary.reviewCount()
            );
        });
    }

    // ============================================================
    // DELETE PRODUCT
    // ============================================================

    @Override
    @Transactional
    public void deleteProduct(Long id) {

        Product product = findProductOrThrow(id);

        for (ProductImage image : product.getProductImages()) {

            if (image.getPublicId() == null) {
                continue;
            }

            try {

                cloudinaryService.deleteImage(
                        image.getPublicId()
                );

            } catch (ImageUploadException e) {

                System.err.println(
                        "Warning: failed to delete Cloudinary image ("
                                + image.getPublicId()
                                + "): "
                                + e.getMessage()
                );
            }
        }

        productAttributeValueRepository.deleteAll(
                productAttributeValueRepository.findByProductId(id)
        );

        productRepository.delete(product);
    }

    // ============================================================
    // LOAD REVIEW SUMMARIES
    // ============================================================

    /**
     * Loads average rating and review count for multiple products
     * using one grouped database query.
     *
     * This prevents:
     *
     * 1 product = 1 review query
     *
     * for product listing pages.
     */
    private Map<Long, ReviewSummary> loadReviewSummaries(
            List<Long> productIds
    ) {

        if (productIds.isEmpty()) {
            return Map.of();
        }

        List<Object[]> rows =
                reviewsRepository.findRatingSummaryByProductIds(
                        productIds
                );

        Map<Long, ReviewSummary> result =
                new HashMap<>();

        for (Object[] row : rows) {

            Long productId =
                    ((Number) row[0]).longValue();

            Float averageRating =
                    row[1] == null
                            ? 0.0f
                            : ((Number) row[1]).floatValue();

            Integer reviewCount =
                    row[2] == null
                            ? 0
                            : ((Number) row[2]).intValue();

            result.put(
                    productId,
                    new ReviewSummary(
                            averageRating,
                            reviewCount
                    )
            );
        }

        return result;
    }

    // ============================================================
    // CALCULATE SINGLE PRODUCT REVIEW SUMMARY
    // ============================================================

    /**
     * Calculates the rating summary for one product.
     *
     * Used by getProductById().
     */
    private ReviewSummary calculateReviewSummary(
            List<Reviews> reviews
    ) {

        if (reviews == null || reviews.isEmpty()) {
            return ReviewSummary.empty();
        }

        int totalRating = 0;
        int reviewCount = 0;

        for (Reviews review : reviews) {

            if (review == null) {
                continue;
            }

            totalRating += review.getRating();
            reviewCount++;
        }

        if (reviewCount == 0) {
            return ReviewSummary.empty();
        }

        float averageRating =
                (float) totalRating / reviewCount;

        return new ReviewSummary(
                averageRating,
                reviewCount
        );
    }

    // ============================================================
    // APPLY PRODUCT DTO
    // ============================================================

    private void applyDto(
            Product product,
            ProductRequestDto dto,
            String normalizedSlug
    ) {

        Category category = categoryRepository
                .findById(dto.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with id: "
                                        + dto.getCategoryId()
                        )
                );

        product.setName(
                dto.getName().trim()
        );

        product.setSlug(
                normalizedSlug
        );

        product.setShortDescription(
                trimToNull(
                        dto.getShortDescription()
                )
        );

        product.setDescription(
                trimToNull(
                        dto.getDescription()
                )
        );

        product.setBasePrice(
                dto.getBasePrice()
        );

        product.setDiscountedPrice(
                dto.getDiscountedPrice()
        );

        product.setIsActive(
                dto.getIsActive() != null
                        ? dto.getIsActive()
                        : Boolean.TRUE
        );

        product.setCategory(category);
    }

    // ============================================================
    // VALIDATE PRICING
    // ============================================================

    private void assertValidPricing(
            BigDecimal basePrice,
            BigDecimal discountedPrice
    ) {

        if (basePrice == null) {

            throw new BusinessRuleViolationException(
                    "Base price is required"
            );
        }

        if (basePrice.compareTo(BigDecimal.ZERO) < 0) {

            throw new BusinessRuleViolationException(
                    "Base price cannot be negative"
            );
        }

        if (discountedPrice != null
                && discountedPrice.compareTo(BigDecimal.ZERO) < 0) {

            throw new BusinessRuleViolationException(
                    "Discounted price cannot be negative"
            );
        }

        if (discountedPrice != null
                && discountedPrice.compareTo(basePrice) > 0) {

            throw new BusinessRuleViolationException(
                    "Discounted price cannot be greater than base price"
            );
        }
    }

    // ============================================================
    // VALIDATE SLUG
    // ============================================================

    private void assertSlugAvailable(
            String slug,
            Long excludeId
    ) {

        boolean taken =
                excludeId == null
                        ? productRepository.existsBySlug(slug)
                        : productRepository.existsBySlugAndIdNot(
                        slug,
                        excludeId
                );

        if (taken) {

            throw new DuplicateResourceException(
                    "A product with slug '"
                            + slug
                            + "' already exists"
            );
        }
    }

    // ============================================================
    // NORMALIZE SLUG
    // ============================================================

    private String normalizeSlug(String slug) {

        if (slug == null || slug.isBlank()) {

            throw new BusinessRuleViolationException(
                    "Slug is required"
            );
        }

        String normalized =
                slug.trim()
                        .toLowerCase(Locale.ROOT)
                        .replaceAll(
                                "[^a-z0-9]+",
                                "-"
                        )
                        .replaceAll(
                                "^-+|-+$",
                                ""
                        );

        if (normalized.isBlank()) {

            throw new BusinessRuleViolationException(
                    "Slug must contain valid characters"
            );
        }

        return normalized;
    }

    // ============================================================
    // TRIM STRING
    // ============================================================

    private String trimToNull(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }

    // ============================================================
    // FIND PRODUCT
    // ============================================================

    private Product findProductOrThrow(Long id) {

        return productRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: "
                                        + id
                        )
                );
    }

    // ============================================================
    // REVIEW SUMMARY
    // ============================================================

    private record ReviewSummary(
            Float avgRating,
            Integer reviewCount
    ) {

        private static ReviewSummary empty() {

            return new ReviewSummary(
                    0.0f,
                    0
            );
        }
    }
}