package com.nikyfarm.mapper;

import com.nikyfarm.dto.AttributeRequestDto;
import com.nikyfarm.dto.AttributeResponseDto;
import com.nikyfarm.entity.Attribute;
import com.nikyfarm.entity.Category;

public final class AttributeMapper {

    private AttributeMapper() {
    }

    public static AttributeResponseDto toResponseDto(Attribute attribute) {
        Category category = attribute.getCategory();
        return new AttributeResponseDto(
                attribute.getId(),
                attribute.getName(),
                attribute.getUnit(),
                attribute.getInputType(),
                attribute.getSortOrder(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                attribute.getOptions() != null ? attribute.getOptions().size() : 0
        );
    }

    /** Pre-fills an edit form with the current values of an existing attribute. */
    public static AttributeRequestDto toRequestDto(AttributeResponseDto dto) {
        AttributeRequestDto request = new AttributeRequestDto();
        request.setName(dto.name());
        request.setUnit(dto.unit());
        request.setInputType(dto.inputType());
        request.setSortOrder(dto.sortOrder());
        request.setCategoryId(dto.categoryId());
        return request;
    }
}
