package com.nikyfarm.dto;

import com.nikyfarm.entity.Attribute;

public record AttributeResponseDto(
        Long id,
        String name,
        String unit,
        Attribute.InputType inputType,
        Integer sortOrder,
        Long categoryId,
        String categoryName,
        int optionCount
) {
}
