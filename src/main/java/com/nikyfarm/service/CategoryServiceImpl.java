package com.nikyfarm.service;

import com.nikyfarm.dto.CategoryRequestDto;
import com.nikyfarm.dto.CategoryResponseDto;
import com.nikyfarm.dto.ImageUploadResult;
import com.nikyfarm.entity.Category;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.CategoryMapper;
import com.nikyfarm.repository.CategoryRepository;
import com.nikyfarm.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final String IMAGE_FOLDER = "categories";
    private static final int MAX_PARENT_CHAIN_DEPTH = 50;

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto dto, MultipartFile file) {
        assertSlugAvailable(dto.getSlug(), null);

        Category category = new Category();
        Category parent = resolveParent(null, dto.getParentId());
        applyDto(category, dto, parent);

        if (file != null && !file.isEmpty()) {
            ImageUploadResult uploaded = cloudinaryService.uploadImage(file, IMAGE_FOLDER);
            category.setImageUrl(uploaded.getImageUrl());
            category.setImagePublicId(uploaded.getPublicId());
        }

        return CategoryMapper.toResponseDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto dto, MultipartFile file) {
        Category category = findCategoryOrThrow(id);
        assertSlugAvailable(dto.getSlug(), id);

        Category parent = resolveParent(id, dto.getParentId());
        applyDto(category, dto, parent);

        if (file != null && !file.isEmpty()) {
            ImageUploadResult uploaded =
                    cloudinaryService.replaceImage(category.getImagePublicId(), file, IMAGE_FOLDER);
            category.setImageUrl(uploaded.getImageUrl());
            category.setImagePublicId(uploaded.getPublicId());
        }

        return CategoryMapper.toResponseDto(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(Long id) {
        return CategoryMapper.toResponseDto(findCategoryOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategoryOrThrow(id);

        if (categoryRepository.existsByParentId(id)) {
            throw new BusinessRuleViolationException("Cannot delete category " + id + " because it has subcategories");
        }
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessRuleViolationException(
                    "Cannot delete category " + id + " because it still has products assigned to it");
        }

        if (category.getImagePublicId() != null) {
            cloudinaryService.deleteImage(category.getImagePublicId());
        }
        categoryRepository.delete(category);
    }

    private void applyDto(Category category, CategoryRequestDto dto, Category resolvedParent) {
        category.setName(dto.getName());
        category.setSlug(dto.getSlug());
        category.setDescription(dto.getDescription());
        category.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        category.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : Boolean.TRUE);
        category.setParent(resolvedParent);
    }

    /**
     * @param categoryId the category being created/updated, or null when creating a brand-new one.
     */
    private Category resolveParent(Long categoryId, Long parentId) {
        if (parentId == null) {
            return null;
        }
        if (parentId.equals(categoryId)) {
            throw new BusinessRuleViolationException("A category cannot be its own parent");
        }
        Category parent = categoryRepository
                .findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + parentId));
        if (categoryId != null) {
            assertNoCycle(categoryId, parent);
        }
        return parent;
    }

    /**
     * Walks up the candidate parent's ancestor chain to make sure {@code categoryId} isn't among
     * them.
     */
    private void assertNoCycle(Long categoryId, Category candidateParent) {
        Category current = candidateParent;
        int depth = 0;
        while (current != null) {
            if (current.getId().equals(categoryId)) {
                throw new BusinessRuleViolationException(
                        "Assigning this parent would create a circular category hierarchy");
            }
            if (++depth > MAX_PARENT_CHAIN_DEPTH) {
                throw new BusinessRuleViolationException("Category hierarchy is too deep");
            }
            current = current.getParent();
        }
    }

    private void assertSlugAvailable(String slug, Long excludeId) {
        boolean taken = excludeId == null
                ? categoryRepository.existsBySlug(slug)
                : categoryRepository.existsBySlugAndIdNot(slug, excludeId);
        if (taken) {
            throw new DuplicateResourceException("A category with slug '" + slug + "' already exists");
        }
    }

    private Category findCategoryOrThrow(Long id) {
        return categoryRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }
}
