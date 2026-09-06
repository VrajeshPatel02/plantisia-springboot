package com.nikyfarm.mapper;

import com.nikyfarm.dto.CategoryRequestDto;
import com.nikyfarm.dto.CategoryResponseDto;
import com.nikyfarm.entity.Category;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryResponseDto toResponseDto(Category category) {
        Category parent = category.getParent();
        return new CategoryResponseDto(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImageUrl(),
                category.getSortOrder(),
                category.getIsActive(),
                parent != null ? parent.getId() : null,
                parent != null ? parent.getName() : null,
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    /** Pre-fills an edit form with the current values of an existing category. */
    public static CategoryRequestDto toRequestDto(CategoryResponseDto dto) {
        CategoryRequestDto request = new CategoryRequestDto();
        request.setName(dto.name());
        request.setSlug(dto.slug());
        request.setDescription(dto.description());
        request.setSortOrder(dto.sortOrder());
        request.setIsActive(dto.isActive());
        request.setParentId(dto.parentId());
        return request;
    }
}
